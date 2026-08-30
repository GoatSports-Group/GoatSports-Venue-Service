package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaymentEventServiceImplTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingQrPort bookingQrPort;

    @Test
    void confirmsBookingAndIssuesSignedQrExactlyOnceForValidDeposit() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingQrPort.issue(bookingId, LocalDateTime.of(
                LocalDate.of(2026, 9, 1), LocalTime.of(9, 0)
        ))).thenReturn("GSQR.v1.signed");
        when(bookingRepository.save(booking)).thenReturn(booking);
        var service = new BookingPaymentEventServiceImpl(bookingRepository, bookingQrPort);
        var event = event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0);

        assertThat(service.applySucceededPayment(event)).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDepositPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getQrCode()).isEqualTo("GSQR.v1.signed");

        assertThat(service.applySucceededPayment(event)).isFalse();
        verify(bookingRepository).save(booking);
    }

    @Test
    void rejectsWrongPayerOrAmountWithoutConfirmingBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        var service = new BookingPaymentEventServiceImpl(bookingRepository, bookingQrPort);

        assertThatThrownBy(() -> service.applySucceededPayment(event(
                UUID.randomUUID(), UUID.randomUUID(), bookingId, "BOOKING_DEPOSIT", 60_000.0
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("Payer");
        assertThatThrownBy(() -> service.applySucceededPayment(event(
                UUID.randomUUID(), payerId, bookingId, "BOOKING_DEPOSIT", 1.0
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("không khớp");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(bookingRepository, never()).save(booking);
    }

    private Booking booking(UUID bookingId, UUID payerId) {
        return Booking.builder()
                .bookingId(bookingId).playerId(payerId).playDate(LocalDate.of(2026, 9, 1))
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .status(BookingStatus.PENDING_PAYMENT).depositAmount(60_000.0)
                .remainingAmount(140_000.0).build();
    }

    private PaymentSucceededCommand event(
            UUID paymentId, UUID payerId, UUID bookingId, String purpose, Double amount
    ) {
        return new PaymentSucceededCommand(
                paymentId, payerId, purpose, "BOOKING", bookingId, amount, "VND", "SUCCEEDED"
        );
    }
}
