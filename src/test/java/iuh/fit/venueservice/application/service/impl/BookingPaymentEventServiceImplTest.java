package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.port.BookingLifecyclePort.LifecycleMessage;
import iuh.fit.venueservice.application.port.BookingLifecyclePort;
import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.BookingTicketMailPort;
import iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaymentEventServiceImplTest {
    private final BookingLifecyclePort bookingLifecyclePort = mock(BookingLifecyclePort.class);
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingQrPort bookingQrPort;
    @Mock private VenueCourtRepository venueCourtRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private TimeSlotLifecycleService timeSlotLifecycleService;
    @Mock private BookingCancellationRepository bookingCancellationRepository;
    @Mock private MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    @Mock private PaymentRefundPort paymentRefundPort;
    @Mock private BookingNotificationPort bookingNotificationPort;
    @Mock private BookingTicketMailPort bookingTicketMailPort;

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
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
        );
        PaymentSucceededCommand event = event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0);

        assertThat(service.applySucceededPayment(event)).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDepositPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getQrCode()).isEqualTo("GSQR.v1.signed");

        assertThat(service.applySucceededPayment(event)).isFalse();
        verify(bookingRepository).save(booking);
        verify(timeSlotLifecycleService, Mockito.times(2)).confirmBookingSlot(booking);
        // Tiền cọc tính vào doanh thu chủ sân (gọi lặp ở sự kiện trùng là vô hại: payment-service bỏ qua).
        verify(paymentRefundPort, Mockito.times(2)).assignPayee(paymentId, ownerId);
        // Chủ sân nhận đúng một thông báo "đơn mới" (sự kiện trùng không báo lại).
        verify(bookingNotificationPort, Mockito.times(1)).notifyOwner(
                ArgumentMatchers.eq(ownerId), ArgumentMatchers.eq(bookingId),
                ArgumentMatchers.eq("booking-confirmed-owner:" + bookingId),
                ArgumentMatchers.eq("Đơn đặt sân mới"),
                ArgumentMatchers.contains("lúc 08:00 ngày 01/09/2026 đã đóng cọc"));
        // Người chơi nhận đúng một email vé, mang đúng token QR vừa cấp.
        verify(bookingTicketMailPort, Mockito.times(1)).sendTicket(ArgumentMatchers.eq(payerId),
                ArgumentMatchers.argThat(ticket -> ticket.bookingId().equals(bookingId)
                        && "GSQR.v1.signed".equals(ticket.qrCode())));
        // Process vòng đời đi tiếp đúng một lần.
        verify(bookingLifecyclePort, Mockito.times(1))
                .publish(bookingId, LifecycleMessage.DEPOSIT_PAID, "deposit-paid:" + paymentId);
    }

    @Test
    void addressLineSkipsBlankParts() {
        assertThat(BookingPaymentEventServiceImpl.addressLine(Address.builder()
                .address("12 Nguyễn Văn Bảo").ward(" ").district("Gò Vấp").city("TP.HCM").build()))
                .isEqualTo("12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM");
        assertThat(BookingPaymentEventServiceImpl.addressLine(Address.builder().build())).isNull();
        assertThat(BookingPaymentEventServiceImpl.addressLine(null)).isNull();
    }

    @Test
    void rejectsWrongPayerOrAmountWithoutConfirmingBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
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
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
        );

        assertThat(service.applySucceededPayment(event(
                paymentId, ownerId, bookingId, "BOOKING_REMAINING", 100_000.0
        ))).isTrue();
        assertThat(booking.getRemainingPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(timeSlotLifecycleService).confirmBookingSlot(booking);
    }

    @Test
    void remindsTheVenueOwnerOnlyWhileTheCashRefundIsStillUnconfirmed() throws Exception {
        UUID refundId = UUID.randomUUID();
        UUID reminderId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        BookingCancellation cancellation = BookingCancellation.builder()
                .refundId(refundId).bookingId(bookingId).status(CancellationStatus.REFUND_MANUAL_REVIEW).build();
        when(bookingCancellationRepository.findByRefundIdForUpdate(refundId)).thenReturn(Optional.of(cancellation));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(Booking.builder()
                .bookingId(bookingId).bookingCode("GSCASH01").venueCourtId(courtId).build()));
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(ownerId).build()));
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
        );

        assertThat(service.remindOwnerOfManualRefund(refundId, reminderId, 60_000)).isTrue();
        verify(bookingNotificationPort).notifyOwner(ArgumentMatchers.eq(ownerId),
                ArgumentMatchers.eq(bookingId),
                ArgumentMatchers.eq("refund-reminder-owner:" + reminderId),
                ArgumentMatchers.eq("Còn khoản hoàn tiền mặt chưa xác nhận"),
                ArgumentMatchers.contains("GSCASH01 đã hủy, khách đang chờ hoàn 60.000 đ"));

        // Chủ sân xác nhận xong trước khi sự kiện tới: không nhắc nữa.
        cancellation.setStatus(CancellationStatus.REFUNDED);
        assertThat(service.remindOwnerOfManualRefund(refundId, UUID.randomUUID(), 60_000)).isFalse();
        verify(bookingNotificationPort, Mockito.times(1)).notifyOwner(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void expiresUnpaidBookingAndReleasesSlotForTerminalPayment() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
        );

        assertThat(service.applyTerminalPayment(new PaymentTerminalCommand(
                UUID.randomUUID(), payerId, "BOOKING_DEPOSIT", "BOOKING", bookingId,
                "EXPIRED", "PAYMENT_EXPIRED", "Đã hết thời gian giữ chỗ thanh toán"
        ))).isTrue();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(timeSlotLifecycleService).releaseBookingSlot(booking);
        verify(bookingRepository).save(booking);
        verify(bookingLifecyclePort).publish(bookingId, LifecycleMessage.PAYMENT_CLOSED, "payment-closed:" + bookingId);
    }

    @Test
    void refundsDepositThatArrivesAfterTheBookingExpired() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        Booking booking = booking(bookingId, payerId);
        booking.setStatus(BookingStatus.EXPIRED);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
        );

        assertThat(service.applySucceededPayment(event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0))).isTrue();

        verify(paymentRefundPort).createRefund(ArgumentMatchers.eq(paymentId),
                ArgumentMatchers.eq(payerId), ArgumentMatchers.eq("unaccepted-payment:" + paymentId),
                ArgumentMatchers.anyString(), ArgumentMatchers.eq(60_000.0),
                ArgumentMatchers.eq(true));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        // Khoản hoàn được ghi lên booking để màn hình kết quả không báo "thành công", và người trả được báo riêng.
        assertThat(booking.getLateRefundPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getLateRefundAmount()).isEqualTo(60_000.0);
        assertThat(booking.getLateRefundAt()).isNotNull();
        verify(bookingRepository).save(booking);
        verify(bookingNotificationPort).notifyPayer(ArgumentMatchers.eq(payerId),
                ArgumentMatchers.eq(bookingId), ArgumentMatchers.eq("late-payment-refund:" + paymentId),
                ArgumentMatchers.eq("Hết giờ giữ sân — đang hoàn tiền"),
                ArgumentMatchers.contains("60.000 đ"));
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
        when(bookingQrPort.issue(ArgumentMatchers.eq(bookingId), ArgumentMatchers.any()))
                .thenReturn("GSQR.v1.signed");
        when(bookingRepository.save(booking)).thenReturn(booking);
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
        );

        assertThat(service.applySucceededPayment(event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0))).isTrue();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDepositPaymentId()).isEqualTo(paymentId);
        assertThat(booking.getLateRefundPaymentId()).isNull();
        verify(timeSlotLifecycleService).confirmBookingSlot(booking);
        verify(paymentRefundPort, never()).createRefund(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any());
        // Khong co thong bao "hoan tien muon" cho nguoi tra; chu san nhan thong bao don moi.
        verify(bookingNotificationPort, never()).notifyPayer(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        verify(bookingNotificationPort).notifyOwner(ArgumentMatchers.any(), ArgumentMatchers.eq(bookingId),
                ArgumentMatchers.eq("booking-confirmed-owner:" + bookingId), ArgumentMatchers.any(),
                ArgumentMatchers.any());
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
        BookingPaymentEventServiceImpl service = new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort, bookingTicketMailPort, bookingLifecyclePort
        );

        assertThat(service.applySucceededPayment(event(paymentId, payerId, bookingId, "BOOKING_DEPOSIT", 60_000.0))).isFalse();

        Mockito.verifyNoInteractions(paymentRefundPort);
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
