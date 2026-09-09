package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.dto.request.payment.RefundResultCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
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
    @Mock private VenueCourtRepository venueCourtRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private TimeSlotLifecycleService timeSlotLifecycleService;
    @Mock private iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository bookingCancellationRepository;

    @Test
    void confirmsBookingAndIssuesSignedQrExactlyOnceForValidDeposit() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingQrPort.issue(bookingId, LocalDateTime.of(
                LocalDate.of(2026, 9, 1), LocalTime.of(9, 0)
        ))).thenReturn("GSQR.v1.signed");
        when(bookingRepository.save(booking)).thenReturn(booking);
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository
        );
        var event = event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0);

        assertThat(service.applySucceededPayment(event)).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDepositPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getQrCode()).isEqualTo("GSQR.v1.signed");

        assertThat(service.applySucceededPayment(event)).isFalse();
        verify(bookingRepository).save(booking);
        verify(timeSlotLifecycleService, org.mockito.Mockito.times(2)).confirmBookingSlot(booking);
    }

    @Test
    void rejectsWrongPayerOrAmountWithoutConfirmingBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository
        );

        assertThatThrownBy(() -> service.applySucceededPayment(event(
                UUID.randomUUID(), UUID.randomUUID(), bookingId, "BOOKING_DEPOSIT", 60_000.0
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("Payer");
        assertThatThrownBy(() -> service.applySucceededPayment(event(
                UUID.randomUUID(), payerId, bookingId, "BOOKING_DEPOSIT", 1.0
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("không khớp");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    void confirmsWalkInWhenOwnerPaysRemainingAmount() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .bookingId(bookingId).venueCourtId(courtId).source(BookingSource.WALK_IN)
                .status(BookingStatus.PENDING_PAYMENT).remainingAmount(100_000.0)
                .playDate(LocalDate.of(2026, 9, 1)).startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0)).build();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()
        ));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(ownerId).build()
        ));
        when(bookingRepository.save(booking)).thenReturn(booking);
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository
        );

        assertThat(service.applySucceededPayment(event(
                paymentId, ownerId, bookingId, "BOOKING_REMAINING", 100_000.0
        ))).isTrue();
        assertThat(booking.getRemainingPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(timeSlotLifecycleService).confirmBookingSlot(booking);
    }

    @Test
    void persistsRefundFailureReasonFromPaymentEvent() throws Exception {
        UUID refundId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        BookingCancellation cancellation = BookingCancellation.builder()
                .refundId(refundId)
                .bookingId(bookingId)
                .status(CancellationStatus.REFUND_PROCESSING)
                .build();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .depositPaymentId(paymentId)
                .status(BookingStatus.REFUND_PENDING)
                .build();
        when(bookingCancellationRepository.findByRefundIdForUpdate(refundId))
                .thenReturn(Optional.of(cancellation));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingCancellationRepository.save(cancellation)).thenReturn(cancellation);
        when(bookingRepository.save(booking)).thenReturn(booking);
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository
        );

        assertThat(service.applyRefundResult(new RefundResultCommand(
                refundId,
                paymentId,
                "FAILED",
                "Không tìm thấy tài khoản nhận chi"
        ))).isTrue();

        assertThat(cancellation.getStatus()).isEqualTo(CancellationStatus.REFUND_FAILED);
        assertThat(cancellation.getRefundFailureReason()).isEqualTo("Không tìm thấy tài khoản nhận chi");
        verify(bookingCancellationRepository).save(cancellation);
    }

    @Test
    void expiresUnpaidBookingAndReleasesSlotForTerminalPayment() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository
        );

        assertThat(service.applyTerminalPayment(new PaymentTerminalCommand(
                UUID.randomUUID(), payerId, "BOOKING_DEPOSIT", "BOOKING", bookingId,
                "EXPIRED", "PAYMENT_EXPIRED", "Đã hết thời gian giữ chỗ thanh toán"
        ))).isTrue();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(timeSlotLifecycleService).releaseBookingSlot(booking);
        verify(bookingRepository).save(booking);
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
