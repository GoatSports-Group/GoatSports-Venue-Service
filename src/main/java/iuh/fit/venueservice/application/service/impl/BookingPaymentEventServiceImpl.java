package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.BookingSource;

import java.time.LocalDateTime;

public class BookingPaymentEventServiceImpl implements BookingPaymentEventService {
    private final BookingRepository bookingRepository;
    private final BookingQrPort bookingQrPort;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final TimeSlotLifecycleService timeSlotLifecycleService;

    public BookingPaymentEventServiceImpl(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingQrPort = bookingQrPort;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
    }

    @Override
    public boolean applySucceededPayment(PaymentSucceededCommand command) throws ServiceException {
        if (command == null || !"SUCCEEDED".equals(command.status())
                || !"BOOKING".equals(command.referenceType())) {
            return false;
        }
        if (command.paymentId() == null || command.referenceId() == null || command.payerId() == null) {
            throw new ServiceException("Payment event thiếu định danh bắt buộc.");
        }
        Booking booking = bookingRepository.findByIdForUpdate(command.referenceId()).orElse(null);
        if (booking == null) return false;
        if (!isExpectedPayer(booking, command.payerId())) {
            throw new ServiceException("Payer của payment event không khớp booking.");
        }
        if (!"VND".equals(command.currency())) {
            throw new ServiceException("Currency của payment event không hợp lệ.");
        }

        if ("BOOKING_DEPOSIT".equals(command.purpose())) {
            validateAmount(command.amount(), booking.getDepositAmount());
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
            timeSlotLifecycleService.confirmBookingSlot(booking);
            if (changed) bookingRepository.save(booking);
            return changed;
        }

        if ("BOOKING_REMAINING".equals(command.purpose())) {
            validateAmount(command.amount(), booking.getRemainingAmount());
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    && booking.getStatus() != BookingStatus.CONFIRMED) {
                return false;
            }
            boolean changed = !command.paymentId().equals(booking.getRemainingPaymentId());
            booking.setRemainingPaymentId(command.paymentId());
            if (booking.getSource() == BookingSource.WALK_IN
                    && booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.CONFIRMED);
                changed = true;
            }
            timeSlotLifecycleService.confirmBookingSlot(booking);
            if (changed) bookingRepository.save(booking);
            return changed;
        }
        return false;
    }

    private boolean isExpectedPayer(Booking booking, java.util.UUID payerId) throws ServiceException {
        if (booking.getSource() != BookingSource.WALK_IN) {
            return booking.getPlayerId() != null && booking.getPlayerId().equals(payerId);
        }
        var court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân của booking walk-in."));
        var venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở của booking walk-in."));
        return payerId.equals(venue.getOwnerId());
    }

    private void validateAmount(Double actual, Double expected) throws ServiceException {
        if (actual == null || expected == null || Math.abs(actual - expected) >= 0.01) {
            throw new ServiceException("Số tiền payment event không khớp booking.");
        }
    }
}
