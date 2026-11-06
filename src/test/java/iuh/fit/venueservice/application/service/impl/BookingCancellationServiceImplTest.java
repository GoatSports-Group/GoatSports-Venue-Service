package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.port.BookingLifecyclePort.LifecycleMessage;
import iuh.fit.venueservice.application.port.BookingLifecyclePort;
import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.WorkflowCancellationStateCommand;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.CancellationWorkflowPort;
import iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationReviewMode;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Trạng thái yêu cầu hủy do Camunda quyết định: venue-service chỉ đổi trạng thái khi process ghi xuống
 * (applyWorkflowState), các thao tác của người dùng chỉ ghi dữ liệu hoặc chuyển quyết định vào process.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingCancellationServiceImplTest {
    private final BookingLifecyclePort bookingLifecyclePort = mock(BookingLifecyclePort.class);
    private static final Instant NOW = Instant.parse("2026-09-02T01:00:00Z"); // 08:00 giờ Việt Nam
    @Mock BookingRepository bookingRepository;
    @Mock BookingCancellationRepository cancellationRepository;
    @Mock VenueCourtRepository venueCourtRepository;
    @Mock VenueRepository venueRepository;
    @Mock TimeSlotRepository timeSlotRepository;
    @Mock PaymentRefundPort paymentRefundPort;
    @Mock MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    @Mock BookingNotificationPort bookingNotificationPort;
    @Mock CancellationWorkflowPort workflow;

    private final UUID playerId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();
    private final UUID courtId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final UUID cancellationId = UUID.randomUUID();
    private final LocalDate playDate = LocalDate.of(2026, 9, 3);
    private Booking booking;
    private TimeSlot slot;
    private BookingCancellationServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new BookingCancellationServiceImpl(bookingRepository, cancellationRepository, venueCourtRepository,
                venueRepository, new TimeSlotLifecycleService(timeSlotRepository, clock), paymentRefundPort,
                matchmakingLifecycleSyncPort, bookingNotificationPort, workflow, clock, Duration.ofHours(1), bookingLifecyclePort);
        booking = Booking.builder()
                .bookingId(bookingId).playerId(playerId).venueCourtId(courtId).bookingCode("GS-001")
                .playDate(playDate).startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(19, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 20, 0))
                .status(BookingStatus.CONFIRMED).depositAmount(200_000.0).depositPaymentId(paymentId)
                .build();
        slot = TimeSlot.builder().venueCourtId(courtId).date(playDate)
                .startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(19, 0)).status(TimeSlotStatus.BOOKED).build();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(Venue.builder().venueId(venueId).ownerId(ownerId)
                .cancellationPolicy(CancellationPolicy.builder()
                        .fullRefundHoursBefore(48).partialRefundHoursBefore(12).partialRefundPercentage(50.0).build())
                .build()));
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, LocalTime.of(18, 0), LocalTime.of(19, 0)))
                .thenReturn(Optional.of(slot));
        when(cancellationRepository.save(any(BookingCancellation.class))).thenAnswer(invocation -> {
            BookingCancellation saved = invocation.getArgument(0);
            if (saved.getCancellationId() == null) saved.setCancellationId(cancellationId);
            return saved;
        });
    }

    @Test
    void requestIsRecordedPendingAndHandedToTheWorkflow() throws Exception {
        when(cancellationRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(workflow.start(any())).thenReturn(4242L);

        BookingCancellation result = service.requestCancellation(new CancelBookingCommand(bookingId, playerId, "Đổi lịch"));

        assertThat(result.getStatus()).isEqualTo(CancellationStatus.PENDING);
        assertThat(result.getReviewMode()).isEqualTo(CancellationReviewMode.OWNER_REVIEW);
        assertThat(result.getRefundAmount()).isEqualTo(100_000.0); // còn 34 giờ: hoàn 50%
        assertThat(result.getWorkflowInstanceKey()).isEqualTo(4242L);
        ArgumentCaptor<CancellationWorkflowPort.StartCancellationWorkflow> start =
                ArgumentCaptor.forClass(CancellationWorkflowPort.StartCancellationWorkflow.class);
        verify(workflow).start(start.capture());
        assertThat(start.getValue().reviewMode()).isEqualTo("OWNER_REVIEW");
        assertThat(start.getValue().playStartAt()).isEqualTo("2026-09-03T18:00+07:00");
        // Không còn tự đổi đơn / trả sân / tạo khoản hoàn ở đây: việc đó là của process.
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);
        verifyNoInteractions(paymentRefundPort);
        verify(bookingNotificationPort).notifyOwner(eq(ownerId), eq(bookingId),
                eq("cancellation-requested-owner:" + cancellationId), eq("Yêu cầu hủy cần bạn duyệt"), contains("GS-001"));
    }

    @Test
    void earlyCancellationIsAutomaticButStillDecidedByTheWorkflow() throws Exception {
        booking.setCreatedAt(LocalDateTime.of(2026, 9, 2, 7, 30));
        when(cancellationRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(workflow.start(any())).thenReturn(1L);

        BookingCancellation result = service.requestCancellation(new CancelBookingCommand(bookingId, playerId, "Đặt nhầm"));

        assertThat(result.getReviewMode()).isEqualTo(CancellationReviewMode.AUTOMATIC);
        assertThat(result.getRefundAmount()).isEqualTo(200_000.0);
        assertThat(result.getStatus()).isEqualTo(CancellationStatus.PENDING);
        verify(workflow).start(eq(new CancellationWorkflowPort.StartCancellationWorkflow(cancellationId, bookingId,
                "AUTOMATIC", 200_000.0, "2026-09-03T18:00+07:00",
                "Tự động chấp thuận trong vòng 60 phút kể từ lúc đặt.", null, null)));
        verifyNoInteractions(paymentRefundPort);
    }

    @Test
    void requestFailsWhenTheWorkflowCannotStartSoNoUntrackedCancellationIsLeft() throws Exception {
        when(cancellationRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(workflow.start(any())).thenThrow(new ServiceException("Hệ thống xử lý hủy sân đang bận."));

        assertThatThrownBy(() -> service.requestCancellation(new CancelBookingCommand(bookingId, playerId, "Đổi lịch")))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đang bận");
        verifyNoInteractions(bookingNotificationPort);
    }

    @Test
    void ownerDecisionGoesIntoTheWorkflowWithoutChangingStatusHere() throws Exception {
        BookingCancellation cancellation = pending();
        when(cancellationRepository.findById(cancellationId)).thenReturn(Optional.of(cancellation));
        when(workflow.decide(cancellationId, true, ownerId, "Đồng ý")).thenReturn(true);

        BookingCancellation result = service.processCancellation(cancellationId, true, ownerId, " Đồng ý ");

        assertThat(result.getStatus()).isEqualTo(CancellationStatus.PENDING);
        verify(workflow).decide(cancellationId, true, ownerId, "Đồng ý");
        verify(cancellationRepository, never()).save(any());
    }

    @Test
    void decisionIsRefusedWhenTheWorkflowNoLongerWaits() throws Exception {
        when(cancellationRepository.findById(cancellationId)).thenReturn(Optional.of(pending()));
        when(workflow.decide(any(), eq(false), any(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.processCancellation(cancellationId, false, ownerId, "Sân kín"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đã được xử lý");
        assertThatThrownBy(() -> service.processCancellation(cancellationId, false, ownerId, " "))
                .hasMessageContaining("lý do từ chối");
    }

    @Test
    void workflowStateReleasesTheSlotOnceAndMovesTheBooking() throws Exception {
        BookingCancellation cancellation = pending();
        when(cancellationRepository.findByIdForUpdate(cancellationId)).thenReturn(Optional.of(cancellation));

        service.applyWorkflowState(state(CancellationStatus.REFUND_AWAITING_CLAIM, 10, ownerId, "Đồng ý"));

        assertThat(cancellation.getStatus()).isEqualTo(CancellationStatus.REFUND_AWAITING_CLAIM);
        assertThat(cancellation.getWorkflowVersion()).isEqualTo(10L);
        assertThat(cancellation.getProcessedBy()).isEqualTo(ownerId);
        assertThat(cancellation.getDecisionReason()).isEqualTo("Đồng ý");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUND_PENDING);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);

        // Khung giờ đã có người khác đặt lại: các lần ghi sau (thử lại, đối soát, bước kế) không được nhả nữa.
        slot.setStatus(TimeSlotStatus.BOOKED);
        service.applyWorkflowState(state(CancellationStatus.REFUND_AWAITING_CLAIM, 10, ownerId, "Đồng ý"));
        service.applyWorkflowState(state(CancellationStatus.REFUNDED, 20, ownerId, "Đồng ý"));

        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);
        assertThat(cancellation.getStatus()).isEqualTo(CancellationStatus.REFUNDED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUNDED);
        verify(matchmakingLifecycleSyncPort).syncBookingLifecycle(any(), eq(bookingId), eq("REFUNDED"), eq("REFUNDED"));
        // Flow đặt sân dừng đúng một lần, ngay khi yêu cầu được chấp thuận.
        verify(bookingLifecyclePort, times(1))
                .publish(bookingId, LifecycleMessage.CANCELLED, "cancelled:" + cancellationId);
    }

    @Test
    void lateWriteWithAnOlderVersionNeverOverridesCamundasNewerStatus() throws Exception {
        BookingCancellation cancellation = pending();
        cancellation.setStatus(CancellationStatus.REFUNDED);
        cancellation.setWorkflowVersion(50L);
        when(cancellationRepository.findByIdForUpdate(cancellationId)).thenReturn(Optional.of(cancellation));

        service.applyWorkflowState(state(CancellationStatus.REFUND_PROCESSING, 40, ownerId, null));

        assertThat(cancellation.getStatus()).isEqualTo(CancellationStatus.REFUNDED);
        assertThat(cancellation.getWorkflowVersion()).isEqualTo(50L);
        verify(cancellationRepository, never()).save(any());
    }

    @Test
    void reconciliationWithTheSameVersionRepairsADivergedRecord() throws Exception {
        BookingCancellation cancellation = pending();
        cancellation.setStatus(CancellationStatus.REFUND_FAILED); // ai đó sửa tay trong DB
        cancellation.setWorkflowVersion(30L);
        when(cancellationRepository.findByIdForUpdate(cancellationId)).thenReturn(Optional.of(cancellation));

        service.applyWorkflowState(state(CancellationStatus.PENDING, 30, null, null));

        assertThat(cancellation.getStatus()).isEqualTo(CancellationStatus.PENDING);
    }

    @Test
    void unknownCancellationIsReportedAsNotFoundSoTheProcessCanStop() {
        when(cancellationRepository.findByIdForUpdate(cancellationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyWorkflowState(state(CancellationStatus.APPROVED, 1, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void workflowRefundIsIdempotentPerCancellationAndLeavesStatusToTheProcess() throws Exception {
        BookingCancellation cancellation = pending();
        when(cancellationRepository.findByIdForUpdate(cancellationId)).thenReturn(Optional.of(cancellation));
        UUID refundId = UUID.randomUUID();
        when(paymentRefundPort.createRefund(any(), any(), any(), any(), any(), any())).thenReturn(new RefundSnapshot(
                refundId, paymentId, ownerId, "Hủy", 100_000.0, null, "PROCESSING", null, null, null, null));

        RefundSnapshot refund = service.createWorkflowRefund(cancellationId, bookingId, ownerId);

        assertThat(refund.status()).isEqualTo("PROCESSING");
        assertThat(cancellation.getRefundId()).isEqualTo(refundId);
        assertThat(cancellation.getStatus()).isEqualTo(CancellationStatus.PENDING);
        verify(paymentRefundPort).createRefund(paymentId, ownerId, "booking-cancellation:" + cancellationId,
                "Hủy booking GS-001: Đổi lịch", 100_000.0, true);
    }

    @Test
    void oldRefundInProgressIsAdoptedWhereItStopped() throws Exception {
        BookingCancellation cancellation = pending();
        UUID refundId = UUID.randomUUID();
        cancellation.setStatus(CancellationStatus.REFUND_MANUAL_REVIEW);
        cancellation.setRefundId(refundId);
        when(cancellationRepository.findByIdForUpdate(cancellationId)).thenReturn(Optional.of(cancellation));
        when(workflow.start(any())).thenReturn(77L);

        service.adoptIntoWorkflow(cancellationId);

        verify(workflow).start(eq(new CancellationWorkflowPort.StartCancellationWorkflow(cancellationId, bookingId,
                "OWNER_REVIEW", 100_000.0, "2026-09-03T18:00+07:00", null, "REFUND_MANUAL_REVIEW", refundId)));
        assertThat(cancellation.getWorkflowInstanceKey()).isEqualTo(77L);
    }

    private BookingCancellation pending() {
        return BookingCancellation.builder()
                .cancellationId(cancellationId).bookingId(bookingId).requestedByUserId(playerId)
                .reason("Đổi lịch").refundAmount(100_000.0).status(CancellationStatus.PENDING)
                .reviewMode(CancellationReviewMode.OWNER_REVIEW)
                .build();
    }

    private WorkflowCancellationStateCommand state(CancellationStatus status, long version, UUID processedBy, String note) {
        return new WorkflowCancellationStateCommand(cancellationId, bookingId, status, version, processedBy, note, null);
    }
}
