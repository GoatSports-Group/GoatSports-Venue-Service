package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.port.BookingLifecyclePort;
import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.CourtHoldRepository;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.CourtHold;
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
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    private final BookingLifecyclePort bookingLifecyclePort = mock(BookingLifecyclePort.class);
    @Mock BookingRepository bookingRepository;
    @Mock TimeSlotRepository timeSlotRepository;
    @Mock BookingCancellationRepository bookingCancellationRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock VenueCourtRepository venueCourtRepository;
    @Mock VenueRepository venueRepository;
    @Mock BookingQrPort bookingQrPort;
    @Mock PaymentRefundPort paymentRefundPort;
    @Mock BookingNotificationPort bookingNotificationPort;
    @Mock MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    @Mock(answer = Answers.CALLS_REAL_METHODS) CourtHoldRepository courtHoldRepository;

    @Test
    void createBookingStaysPendingPaymentWithoutFakeProviderSuccess() throws Exception {
        openVenue();
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
        when(bookingLifecyclePort.start(any())).thenReturn(42L);
        BookingServiceImpl service = new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository,
                bookingLifecyclePort
        );

        Booking result = service.createBooking(new CreateBookingCommand(
                UUID.randomUUID(), courtId, playDate, startTime, endTime,
                BookingSource.DIRECT, null, null, null
        ));

        // Process "Vòng đời đặt sân" mở ngay khi tạo đơn, bắt đầu ở bước chờ cọc với hạn giữ chỗ của đơn.
        assertThat(result.getLifecycleInstanceKey()).isEqualTo(42L);
        verify(bookingLifecyclePort).start(argThat(command -> "PENDING_PAYMENT".equals(command.status())
                && "2026-09-01T08:15+07:00".equals(command.holdExpiresAt())));
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
        openVenue();
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
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository,
                bookingLifecyclePort
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
        openVenue();
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
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository,
                bookingLifecyclePort
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

    private BookingServiceImpl serviceAt(Instant instant) {
        Clock clock = Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
        return new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, bookingQrPort, clock, Duration.ofMinutes(15), courtHoldRepository,
                bookingLifecyclePort
        );
    }

    @Test
    void bookingIsRejectedWhileCourtIsHeldForTournament() {
        openVenue();
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
                CourtHold.builder().tournamentId(UUID.randomUUID())
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
                CourtHold.builder().tournamentId(UUID.randomUUID())
                        .venueCourtId(courtId).fromDate(playDate).toDate(playDate)
                        .dailyStart(LocalTime.of(17, 0)).dailyEnd(LocalTime.of(22, 0)).build()));

        List<TimeSlotResponseCommand> slots = serviceAt(Instant.parse("2026-09-01T01:00:00Z")).getAvailableSlots(courtId, playDate);

        assertThat(slots.get(0).status()).isEqualTo("AVAILABLE");
        assertThat(slots.get(1).status()).isEqualTo("TOURNAMENT");
    }

    /** Cơ sở của sân con đang mở (test không quan tâm tới cơ sở). */
    private void openVenue() {
        Mockito.lenient().when(venueRepository.findById(any()))
                .thenReturn(Optional.of(Venue.builder().active(true).build()));
    }

    @Test
    void bookingIsRejectedWhenVenueIsSuspended() {
        UUID courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).active(true).build()));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).active(false).suspendedAt(LocalDateTime.now()).build()));

        assertThatThrownBy(() -> serviceAt(Instant.parse("2026-09-01T01:00:00Z")).createBooking(new CreateBookingCommand(
                UUID.randomUUID(), courtId, LocalDate.of(2026, 9, 3), LocalTime.of(18, 0), LocalTime.of(19, 0),
                BookingSource.DIRECT, null, null, null)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("tạm ngừng nhận đặt sân");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void upcomingHistoryFiltersByPlayTimeNotOnlyByStatus() throws Exception {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-06T05:00:00Z");
        when(bookingRepository.findPlayerUpcomingPage(playerId, LocalDateTime.of(2026, 10, 6, 12, 0), 0, 12))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 12, 0));

        serviceAt(now).getPlayerBookingHistory(playerId, "UPCOMING", 0, 12);

        verify(bookingRepository).findPlayerUpcomingPage(playerId, LocalDateTime.of(2026, 10, 6, 12, 0), 0, 12);
        verify(bookingRepository, never()).findPlayerPage(any(), any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
    }
}
