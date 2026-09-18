package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.dto.request.payment.RefundResultCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.CancellationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class BookingPaymentEventServiceImpl implements BookingPaymentEventService {
    private final BookingRepository bookingRepository;
    private final BookingQrPort bookingQrPort;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;

    public BookingPaymentEventServiceImpl(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            BookingCancellationRepository bookingCancellationRepository,
            MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingQrPort = bookingQrPort;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
        this.bookingCancellationRepository = bookingCancellationRepository;
        this.matchmakingLifecycleSyncPort = matchmakingLifecycleSyncPort;
    }

    @Override
    public boolean applyTerminalPayment(PaymentTerminalCommand command) throws ServiceException {
        if (command == null || !command.isBookingDepositTerminal()) {
            return false;
        }
        command.validateIdentity();

        Booking booking = bookingRepository.findByIdForUpdate(command.referenceId()).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return false;
        }
        if (!isExpectedPayer(booking, command.payerId())) {
            throw new ServiceException("Payer của payment terminal event không khớp booking.");
        }

        boolean paymentCancelled = "CANCELLED".equals(command.status());
        booking.setStatus(paymentCancelled ? BookingStatus.CANCELLED : BookingStatus.EXPIRED);
        timeSlotLifecycleService.releaseBookingSlot(booking);
        Booking saved = bookingRepository.save(booking);
        matchmakingLifecycleSyncPort.syncBookingLifecycle(
                saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name(),
                paymentCancelled ? "PAYMENT_CANCELLED" : "PAYMENT_EXPIRED"
        );
        return true;
    }

    @Override
    public boolean applyRefundResult(RefundResultCommand command) throws ServiceException {
        command.validateIdentity();
        var cancellation = bookingCancellationRepository.findByRefundIdForUpdate(command.refundId()).orElse(null);
        if (cancellation == null) return false;
        Booking booking = bookingRepository.findByIdForUpdate(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy booking của refund."));
        if (!command.paymentId().equals(booking.getDepositPaymentId())) {
            throw new ServiceException("Payment của refund event không khớp booking.");
        }
        CancellationStatus status = switch (command.status()) {
            case "SUCCEEDED" -> CancellationStatus.REFUNDED;
            case "FAILED" -> CancellationStatus.REFUND_FAILED;
            case "AWAITING_BANK_ACCOUNT" -> CancellationStatus.REFUND_AWAITING_BANK_ACCOUNT;
            case "AWAITING_CLAIM" -> CancellationStatus.REFUND_AWAITING_CLAIM;
            case "MANUAL_REVIEW" -> CancellationStatus.REFUND_MANUAL_REVIEW;
            default -> CancellationStatus.REFUND_PROCESSING;
        };
        cancellation.setStatus(status);
        cancellation.setRefundFailureReason(
                status == CancellationStatus.REFUND_FAILED
                        || status == CancellationStatus.REFUND_AWAITING_BANK_ACCOUNT
                        ? command.failureReason()
                        : null
        );
        booking.setStatus(status == CancellationStatus.REFUNDED
                ? BookingStatus.REFUNDED : BookingStatus.REFUND_PENDING);
        bookingCancellationRepository.save(cancellation);
        Booking saved = bookingRepository.save(booking);
        if (status == CancellationStatus.REFUNDED) {
            matchmakingLifecycleSyncPort.syncBookingLifecycle(
                    saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name(), "REFUNDED"
            );
        }
        return true;
    }

    @Override
    public boolean applySucceededPayment(PaymentSucceededCommand command) throws ServiceException {
        if (command == null || !command.isBookingPaymentSucceeded()) {
            return false;
        }
        command.validateIdentity();
        Booking booking = bookingRepository.findByIdForUpdate(command.referenceId()).orElse(null);
        if (booking == null) return false;
        if (!isExpectedPayer(booking, command.payerId())) {
            throw new ServiceException("Payer của payment event không khớp booking.");
        }

        if ("BOOKING_DEPOSIT".equals(command.purpose())) {
            ensureAmountMatches(command.amount(), booking.getDepositAmount());
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    && booking.getStatus() != BookingStatus.CONFIRMED) {
                return false;
            }
            boolean changed = !command.paymentId().equals(booking.getDepositPaymentId());
            booking.setDepositPaymentId(command.paymentId());
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.CONFIRMED);
                changed = true;
            }
            if (booking.getQrCode() == null || booking.getQrCode().isBlank()) {
                booking.setQrCode(bookingQrPort.issue(
                        booking.getBookingId(), LocalDateTime.of(booking.getPlayDate(), booking.getEndTime())
                ));
                changed = true;
            }
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                    || booking.getStatus() == BookingStatus.CONFIRMED) {
                timeSlotLifecycleService.confirmBookingSlot(booking);
            }
            if (changed) {
                Booking saved = bookingRepository.save(booking);
                matchmakingLifecycleSyncPort.syncBookingLifecycle(
                        saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name()
                );
            }
            return changed;
        }

        if ("BOOKING_REMAINING".equals(command.purpose())) {
            ensureAmountMatches(command.amount(), booking.getRemainingAmount());
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    && booking.getStatus() != BookingStatus.CONFIRMED
                    && booking.getStatus() != BookingStatus.CHECKED_IN
                    && booking.getStatus() != BookingStatus.COMPLETED) {
                return false;
            }
            boolean changed = !command.paymentId().equals(booking.getRemainingPaymentId());
            booking.setRemainingPaymentId(command.paymentId());
            if (booking.getSource() == BookingSource.WALK_IN
                    && booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.CONFIRMED);
                changed = true;
            }
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                    || booking.getStatus() == BookingStatus.CONFIRMED) {
                timeSlotLifecycleService.confirmBookingSlot(booking);
            }
            if (changed) {
                Booking saved = bookingRepository.save(booking);
                matchmakingLifecycleSyncPort.syncBookingLifecycle(
                        saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name()
                );
            }
            return changed;
        }
        return false;
    }

    private boolean isExpectedPayer(Booking booking, UUID payerId) throws ServiceException {
        if (booking.getSource() != BookingSource.WALK_IN) {
            return booking.getPlayerId() != null && booking.getPlayerId().equals(payerId);
        }
        var court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân của booking walk-in."));
        var venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở của booking walk-in."));
        return payerId.equals(venue.getOwnerId());
    }

    private void ensureAmountMatches(Double actual, Double expected) throws ServiceException {
        if (actual == null || expected == null || Math.abs(actual - expected) >= 0.01) {
            throw new ServiceException("Số tiền payment event không khớp booking.");
        }
    }
}
