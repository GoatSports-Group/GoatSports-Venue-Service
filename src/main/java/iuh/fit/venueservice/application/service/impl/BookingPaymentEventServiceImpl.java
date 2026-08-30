package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDateTime;

public class BookingPaymentEventServiceImpl implements BookingPaymentEventService {
    private final BookingRepository bookingRepository;
    private final BookingQrPort bookingQrPort;

    public BookingPaymentEventServiceImpl(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingQrPort = bookingQrPort;
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
        Booking booking = bookingRepository.findById(command.referenceId()).orElse(null);
        if (booking == null) return false;
        if (booking.getPlayerId() == null || !booking.getPlayerId().equals(command.payerId())) {
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
            if (changed) bookingRepository.save(booking);
            return changed;
        }

        if ("BOOKING_REMAINING".equals(command.purpose())) {
            validateAmount(command.amount(), booking.getRemainingAmount());
            if (command.paymentId().equals(booking.getRemainingPaymentId())) return false;
            booking.setRemainingPaymentId(command.paymentId());
            bookingRepository.save(booking);
            return true;
        }
        return false;
    }

    private void validateAmount(Double actual, Double expected) throws ServiceException {
        if (actual == null || expected == null || Math.abs(actual - expected) >= 0.01) {
            throw new ServiceException("Số tiền payment event không khớp booking.");
        }
    }
}
