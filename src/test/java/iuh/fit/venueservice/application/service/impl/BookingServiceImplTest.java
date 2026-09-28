package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
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
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import iuh.fit.venueservice.domain.enums.CancellationReviewMode;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock BookingRepository bookingRepository;
    @Mock TimeSlotRepository timeSlotRepository;
    @Mock BookingCancellationRepository bookingCancellationRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock VenueCourtRepository venueCourtRepository;
    @Mock VenueRepository venueRepository;
    @Mock BookingQrPort bookingQrPort;
    @Mock PaymentRefundPort paymentRefundPort;
    @Mock iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    @Mock(answer = org.mockito.Answers.CALLS_REAL_METHODS) iuh.fit.venueservice.application.port.persistence.CourtHoldRepository courtHoldRepository;

    @Test
    void createBookingStaysPendingPaymentWithoutFakeProviderSuccess() throws Exception {
        UUID courtId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate playDate = LocalDate.now(clock).plusDays(1);
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(19, 0);

        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).active(true).build()
        ));
        TimeSlot slot = TimeSlot.builder()
                        .venueCourtId(courtId)
                        .date(playDate)
                        .startTime(startTime)
                        .endTime(endTime)
                        .pricePerHour(200_000.0)
                        .status(TimeSlotStatus.AVAILABLE)
                        .build();
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(slot)).thenReturn(slot);
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getBookingId() == null) booking.setBookingId(bookingId);
            return booking;
        });
        BookingServiceImpl service = new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository
        );

        Booking result = service.createBooking(new CreateBookingCommand(
                UUID.randomUUID(), courtId, playDate, startTime, endTime,
                BookingSource.DIRECT, null, null, null
        ));

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(result.getDepositPaymentId()).isNull();
        assertThat(result.getRemainingPaymentId()).isNull();
        assertThat(result.getQrCode()).isNull();
        assertThat(result.getTotalPrice()).isEqualTo(200_000.0);
        assertThat(result.getHoldExpiresAt()).isEqualTo(LocalDate.now(clock).atTime(8, 15));
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.LOCKED);
    }

    @Test
    void createWalkInConfirmsImmediatelyWithoutPaymentHold() throws Exception {
        UUID courtId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate playDate = LocalDate.now(clock).plusDays(1);
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(19, 0);

        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).active(true).build()
        ));
        TimeSlot slot = TimeSlot.builder()
                .venueCourtId(courtId)
                .date(playDate)
                .startTime(startTime)
                .endTime(endTime)
                .pricePerHour(200_000.0)
                .status(TimeSlotStatus.AVAILABLE)
                .build();
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(slot)).thenReturn(slot);
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getBookingId() == null) booking.setBookingId(bookingId);
            return booking;
        });
        when(bookingQrPort.issue(bookingId, playDate.atTime(endTime))).thenReturn("GSQR.v1.walk-in");

        BookingServiceImpl service = new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository
        );

        Booking result = service.createBooking(new CreateBookingCommand(
                null, courtId, playDate, startTime, endTime,
                BookingSource.WALK_IN, "Khách tại quầy", "0901234567", null
        ));

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getDepositAmount()).isZero();
        assertThat(result.getRemainingAmount()).isEqualTo(200_000.0);
        assertThat(result.getHoldExpiresAt()).isNull();
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);
    }

    @Test
    void createBookingRejectsSlotAlreadyHeldByAnotherBooking() {
        UUID courtId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate playDate = LocalDate.now(clock).plusDays(1);
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(19, 0);

        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).active(true).build()
        ));
        TimeSlot heldSlot = TimeSlot.builder()
                .venueCourtId(courtId)
                .date(playDate)
                .startTime(startTime)
                .endTime(endTime)
                .pricePerHour(200_000.0)
                .status(TimeSlotStatus.LOCKED)
                .build();
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(heldSlot));
        BookingServiceImpl service = new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository
        );

        assertThatThrownBy(() -> service.createBooking(new CreateBookingCommand(
                UUID.randomUUID(), courtId, playDate, startTime, endTime,
                BookingSource.DIRECT, null, null, null
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("không còn khả dụng");

        assertThat(heldSlot.getStatus()).isEqualTo(TimeSlotStatus.LOCKED);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void playerCannotReadAnotherPlayersBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Booking booking = Booking.builder().bookingId(bookingId).playerId(ownerId).build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingServiceImpl service = serviceAt(Instant.parse("2026-09-01T01:00:00Z"));

        assertThatThrownBy(() -> service.getPlayerBooking(bookingId, UUID.randomUUID()))
                .isInstanceOf(OwnershipException.class);
    }

    @Test
    void cancellationUsesVenuePolicyAndPaidDeposit() throws Exception {
        UUID playerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .playerId(playerId)
                .venueCourtId(courtId)
                .playDate(LocalDate.of(2026, 9, 3))
                .startTime(LocalTime.of(18, 0))
                .status(BookingStatus.CONFIRMED)
                .depositAmount(200_000.0)
                .depositPaymentId(UUID.randomUUID())
                .build();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingCancellationRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()
        ));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(Venue.builder()
                .venueId(venueId)
                .cancellationPolicy(CancellationPolicy.builder()
                        .fullRefundHoursBefore(24)
                        .partialRefundHoursBefore(12)
                        .partialRefundPercentage(50.0)
                        .build())
                .build()));
        when(bookingCancellationRepository.save(any(BookingCancellation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingCancellation result = cancellationServiceAt(Instant.parse("2026-09-02T22:00:00Z"))
                .requestCancellation(new CancelBookingCommand(bookingId, playerId, "Đổi lịch thi đấu"));

        assertThat(result.getRefundPercentage()).isEqualTo(50.0);
        assertThat(result.getRefundAmount()).isEqualTo(100_000.0);
        assertThat(result.getStatus()).isEqualTo(CancellationStatus.PENDING);
    }

    @Test
    void cancellationWithinOneHourIsAutomaticallyApprovedAndRefundIsCreated() throws Exception {
        UUID playerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID cancellationId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        LocalDate playDate = LocalDate.of(2026, 9, 3);
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(19, 0);
        Booking booking = Booking.builder()
                .bookingId(bookingId).playerId(playerId).venueCourtId(courtId)
                .playDate(playDate).startTime(startTime).endTime(endTime)
                .createdAt(LocalDateTime.of(2026, 9, 2, 7, 30))
                .status(BookingStatus.CONFIRMED).bookingCode("GS-AUTO")
                .depositAmount(200_000.0).depositPaymentId(paymentId)
                .build();
        TimeSlot slot = TimeSlot.builder()
                .venueCourtId(courtId).date(playDate).startTime(startTime).endTime(endTime)
                .status(TimeSlotStatus.BOOKED).build();
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookingCancellationRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()
        ));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).build()
        ));
        when(bookingCancellationRepository.save(any(BookingCancellation.class))).thenAnswer(invocation -> {
            BookingCancellation cancellation = invocation.getArgument(0);
            if (cancellation.getCancellationId() == null) cancellation.setCancellationId(cancellationId);
            return cancellation;
        });
        when(paymentRefundPort.createRefund(any(), any(), any(), any(), any(), any()))
                .thenReturn(new RefundSnapshot(
                        refundId, paymentId, playerId, "Hủy booking", 200_000.0,
                        null, "AWAITING_CLAIM", null, null, null, null
                ));
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, startTime, endTime))
                .thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(slot)).thenReturn(slot);
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingCancellation result = cancellationServiceAt(Instant.parse("2026-09-02T01:00:00Z"))
                .requestCancellation(new CancelBookingCommand(bookingId, playerId, "Đặt nhầm lịch"));

        assertThat(result.getReviewMode()).isEqualTo(CancellationReviewMode.AUTOMATIC);
        assertThat(result.getRefundPercentage()).isEqualTo(100.0);
        assertThat(result.getRefundAmount()).isEqualTo(200_000.0);
        assertThat(result.getStatus()).isEqualTo(CancellationStatus.REFUND_AWAITING_CLAIM);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUND_PENDING);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
        verify(paymentRefundPort).createRefund(
                paymentId, playerId, "booking-cancellation:" + cancellationId,
                "Hủy booking GS-AUTO: Đặt nhầm lịch", 200_000.0, false
        );
    }

    @Test
    void approvalCreatesIdempotentRefundAndUpdatesBooking() throws Exception {
        UUID cancellationId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        BookingCancellation cancellation = BookingCancellation.builder()
                .cancellationId(cancellationId)
                .bookingId(bookingId)
                .reason("Đổi lịch")
                .refundAmount(100_000.0)
                .status(CancellationStatus.PENDING)
                .build();
        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .bookingCode("GS-001")
                .venueCourtId(courtId)
                .playDate(LocalDate.of(2026, 9, 3))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .depositPaymentId(paymentId)
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookingCancellationRepository.findByIdForUpdate(cancellationId)).thenReturn(Optional.of(cancellation));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()
        ));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(ownerId).build()
        ));
        when(paymentRefundPort.createRefund(any(), any(), any(), any(), any(), any()))
                .thenReturn(new RefundSnapshot(
                        refundId, paymentId, ownerId, "Đổi lịch", 100_000.0,
                        null, "SUCCEEDED", null,
                        LocalDateTime.of(2026, 9, 2, 8, 0), null, null
                ));
        when(bookingCancellationRepository.save(cancellation)).thenReturn(cancellation);
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingCancellation result = cancellationServiceAt(Instant.parse("2026-09-02T01:00:00Z"))
                .processCancellation(cancellationId, true, ownerId, "Đồng ý");

        assertThat(result.getRefundId()).isEqualTo(refundId);
        assertThat(result.getStatus()).isEqualTo(CancellationStatus.REFUNDED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUNDED);
        verify(paymentRefundPort).createRefund(
                paymentId, ownerId, "booking-cancellation:" + cancellationId,
                "Hủy booking GS-001: Đổi lịch", 100_000.0, true
        );
    }

    private BookingCancellationServiceImpl cancellationServiceAt(Instant instant) {
        Clock clock = Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
        return new BookingCancellationServiceImpl(
                bookingRepository, bookingCancellationRepository, venueCourtRepository, venueRepository,
                new TimeSlotLifecycleService(timeSlotRepository, clock), paymentRefundPort, matchmakingLifecycleSyncPort,
                clock, Duration.ofHours(1)
        );
    }

    private BookingServiceImpl serviceAt(Instant instant) {
        Clock clock = Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
        return new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository
        );
    }

    @Test
    void bookingIsRejectedWhileCourtIsHeldForTournament() {
        UUID courtId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2026-09-01T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate playDate = LocalDate.now(clock).plusDays(2);
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).active(true).build()));
        TimeSlot slot = TimeSlot.builder().venueCourtId(courtId).date(playDate)
                .startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(19, 0))
                .pricePerHour(200_000.0).status(TimeSlotStatus.AVAILABLE).build();
        when(timeSlotRepository.findByCourtAndTimeForUpdate(courtId, playDate, LocalTime.of(18, 0), LocalTime.of(19, 0)))
                .thenReturn(Optional.of(slot));
        when(courtHoldRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
                iuh.fit.venueservice.domain.entity.CourtHold.builder().tournamentId(UUID.randomUUID())
                        .venueCourtId(courtId).fromDate(playDate.minusDays(1)).toDate(playDate.plusDays(1))
                        .dailyStart(LocalTime.of(17, 0)).dailyEnd(LocalTime.of(22, 0)).build()));

        assertThatThrownBy(() -> serviceAt(Instant.parse("2026-09-01T01:00:00Z")).createBooking(new CreateBookingCommand(
                UUID.randomUUID(), courtId, playDate, LocalTime.of(18, 0), LocalTime.of(19, 0),
                BookingSource.DIRECT, null, null, null)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("giải đấu");
        verify(bookingRepository, never()).save(any());
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
    }

    @Test
    void slotsOutsideTournamentWindowStayBookable() throws Exception {
        UUID courtId = UUID.randomUUID();
        LocalDate playDate = LocalDate.of(2026, 9, 3);
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).active(true).build()));
        when(timeSlotRepository.findAvailableSlots(courtId, playDate)).thenReturn(List.of(
                TimeSlot.builder().timeSlotId(UUID.randomUUID()).venueCourtId(courtId).date(playDate)
                        .startTime(LocalTime.of(6, 0)).endTime(LocalTime.of(7, 0))
                        .pricePerHour(100_000.0).status(TimeSlotStatus.AVAILABLE).build(),
                TimeSlot.builder().timeSlotId(UUID.randomUUID()).venueCourtId(courtId).date(playDate)
                        .startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(19, 0))
                        .pricePerHour(100_000.0).status(TimeSlotStatus.AVAILABLE).build()));
        when(courtHoldRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(
                iuh.fit.venueservice.domain.entity.CourtHold.builder().tournamentId(UUID.randomUUID())
                        .venueCourtId(courtId).fromDate(playDate).toDate(playDate)
                        .dailyStart(LocalTime.of(17, 0)).dailyEnd(LocalTime.of(22, 0)).build()));

        var slots = serviceAt(Instant.parse("2026-09-01T01:00:00Z")).getAvailableSlots(courtId, playDate);

        assertThat(slots.get(0).status()).isEqualTo("AVAILABLE");
        assertThat(slots.get(1).status()).isEqualTo("TOURNAMENT");
    }
}
