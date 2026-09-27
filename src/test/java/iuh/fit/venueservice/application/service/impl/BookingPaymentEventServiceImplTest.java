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
    @Mock private iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    @Mock private iuh.fit.venueservice.application.port.PaymentRefundPort paymentRefundPort;
    @Mock private iuh.fit.venueservice.application.port.BookingNotificationPort bookingNotificationPort;

    @Test
    void confirmsBookingAndIssuesSignedQrExactlyOnceForValidDeposit() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        booking.setVenueCourtId(courtId);
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(ownerId).build()));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingQrPort.issue(bookingId, LocalDateTime.of(
                LocalDate.of(2026, 9, 1), LocalTime.of(9, 0)
        ))).thenReturn("GSQR.v1.signed");
        when(bookingRepository.save(booking)).thenReturn(booking);
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
        );
        var event = event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0);

        assertThat(service.applySucceededPayment(event)).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDepositPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getQrCode()).isEqualTo("GSQR.v1.signed");

        assertThat(service.applySucceededPayment(event)).isFalse();
        verify(bookingRepository).save(booking);
        verify(timeSlotLifecycleService, org.mockito.Mockito.times(2)).confirmBookingSlot(booking);
        // Tiền cọc tính vào doanh thu chủ sân (gọi lặp ở sự kiện trùng là vô hại: payment-service bỏ qua).
        verify(paymentRefundPort, org.mockito.Mockito.times(2)).assignPayee(paymentId, ownerId);
    }

    @Test
    void rejectsWrongPayerOrAmountWithoutConfirmingBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
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
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
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
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
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
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
        );

        assertThat(service.applyTerminalPayment(new PaymentTerminalCommand(
                UUID.randomUUID(), payerId, "BOOKING_DEPOSIT", "BOOKING", bookingId,
                "EXPIRED", "PAYMENT_EXPIRED", "Đã hết thời gian giữ chỗ thanh toán"
        ))).isTrue();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(timeSlotLifecycleService).releaseBookingSlot(booking);
        verify(bookingRepository).save(booking);
    }

    @Test
    void refundsDepositThatArrivesAfterTheBookingExpired() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        booking.setStatus(BookingStatus.EXPIRED);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
        );

        assertThat(service.applySucceededPayment(event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0))).isTrue();

        org.mockito.Mockito.verify(paymentRefundPort).createRefund(org.mockito.ArgumentMatchers.eq(paymentId),
                org.mockito.ArgumentMatchers.eq(payerId), org.mockito.ArgumentMatchers.eq("unaccepted-payment:" + paymentId),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(60_000.0),
                org.mockito.ArgumentMatchers.eq(true));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        // Khoản hoàn được ghi lên booking để màn hình kết quả không báo "thành công", và người trả được báo riêng.
        assertThat(booking.getLateRefundPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getLateRefundAmount()).isEqualTo(60_000.0);
        assertThat(booking.getLateRefundAt()).isNotNull();
        verify(bookingRepository).save(booking);
        verify(bookingNotificationPort).notifyPayer(org.mockito.ArgumentMatchers.eq(payerId),
                org.mockito.ArgumentMatchers.eq(bookingId), org.mockito.ArgumentMatchers.eq("late-payment-refund:" + paymentId),
                org.mockito.ArgumentMatchers.eq("Hết giờ giữ sân — đang hoàn tiền"),
                org.mockito.ArgumentMatchers.contains("60.000 đ"));
    }

    @Test
    void confirmsDepositPaidAfterHoldTimeWhileExpiryJobHasNotRunYet() throws Exception {
        // Tiền về sau holdExpiresAt nhưng job hết hạn (30s/lần) chưa kịp khóa booking: vẫn PENDING_PAYMENT nên nhận,
        // không hoàn — cả hai cùng khóa dòng booking nên chỉ một bên thắng.
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        booking.setVenueCourtId(courtId);
        booking.setHoldExpiresAt(LocalDateTime.now().minusSeconds(20));
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(UUID.randomUUID()).build()));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingQrPort.issue(org.mockito.ArgumentMatchers.eq(bookingId), org.mockito.ArgumentMatchers.any()))
                .thenReturn("GSQR.v1.signed");
        when(bookingRepository.save(booking)).thenReturn(booking);
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
        );

        assertThat(service.applySucceededPayment(event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0))).isTrue();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDepositPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getLateRefundPaymentId()).isNull();
        verify(timeSlotLifecycleService).confirmBookingSlot(booking);
        verify(paymentRefundPort, never()).createRefund(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verifyNoInteractions(bookingNotificationPort);
    }

    @Test
    void doesNotRefundAgainWhenDepositEventOfCancelledBookingIsRedelivered() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setDepositPaymentId(paymentId);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        var service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
        );

        assertThat(service.applySucceededPayment(event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0))).isFalse();

        org.mockito.Mockito.verifyNoInteractions(paymentRefundPort);
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
