package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.CourtHoldRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TournamentCourtHoldService.HoldCourtsCommand;
import iuh.fit.venueservice.domain.entity.CourtHold;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentCourtHoldServiceImplTest {
    @Mock VenueRepository venueRepository;
    @Mock VenueCourtRepository venueCourtRepository;
    @Mock TimeSlotRepository timeSlotRepository;
    @Mock CourtHoldRepository courtHoldRepository;

    private TournamentCourtHoldServiceImpl service;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID tournamentId = UUID.randomUUID();
    private final UUID court1 = UUID.randomUUID();
    private final UUID court2 = UUID.randomUUID();
    private final UUID tennisCourt = UUID.randomUUID();
    private final LocalDate from = LocalDate.of(2026, 10, 3);
    private final LocalDate to = LocalDate.of(2026, 10, 4);

    @BeforeEach
    void setUp() {
        service = new TournamentCourtHoldServiceImpl(venueRepository, venueCourtRepository, timeSlotRepository, courtHoldRepository);
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(Venue.builder().venueId(venueId).ownerId(ownerId)
                .active(true).openTime(LocalTime.of(6, 0)).closeTime(LocalTime.of(22, 0)).build()));
        when(venueCourtRepository.findByVenueId(venueId)).thenReturn(List.of(
                court(court1, "Sân 1", SportType.BADMINTON), court(court2, "Sân 2", SportType.BADMINTON),
                court(tennisCourt, "Sân Tennis", SportType.TENNIS)));
        when(courtHoldRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
    }

    private VenueCourt court(UUID id, String name, SportType sport) {
        return VenueCourt.builder().venueCourtId(id).venueId(venueId).name(name).sportType(sport).active(true).build();
    }

    private HoldCourtsCommand command(UUID owner, SportType sport, List<UUID> courts, LocalTime start, LocalTime end) {
        return new HoldCourtsCommand(tournamentId, owner, venueId, sport, courts, from, to, start, end);
    }

    @Test
    void holdsSelectedCourtsForTheWholeTournament() throws ServiceException {
        List<CourtHold> holds = service.replace(command(ownerId, SportType.BADMINTON, List.of(court1, court2),
                LocalTime.of(8, 0), LocalTime.of(20, 0)), false);

        assertThat(holds).hasSize(2);
        assertThat(holds).allSatisfy(hold -> {
            assertThat(hold.getFromDate()).isEqualTo(from);
            assertThat(hold.getToDate()).isEqualTo(to);
        });
        verify(courtHoldRepository).deleteByTournamentId(tournamentId);
    }

    @Test
    void anotherOwnerCannotHoldCourts() {
        assertThatThrownBy(() -> service.replace(command(UUID.randomUUID(), SportType.BADMINTON, List.of(court1),
                LocalTime.of(8, 0), LocalTime.of(20, 0)), false))
                .isInstanceOf(ServiceException.class).hasMessageContaining("cơ sở của chính mình");
    }

    @Test
    void venueWithoutCourtsOfTheSportCannotHostIt() {
        assertThatThrownBy(() -> service.replace(command(ownerId, SportType.FOOTBALL, List.of(court1),
                LocalTime.of(8, 0), LocalTime.of(20, 0)), false))
                .isInstanceOf(ServiceException.class).hasMessageContaining("chưa có sân");
    }

    @Test
    void courtOfAnotherSportIsRejected() {
        assertThatThrownBy(() -> service.replace(command(ownerId, SportType.BADMINTON, List.of(court1, tennisCourt),
                LocalTime.of(8, 0), LocalTime.of(20, 0)), false))
                .isInstanceOf(ServiceException.class).hasMessageContaining("Sân Tennis");
    }

    @Test
    void windowMustFitOpeningHours() {
        assertThatThrownBy(() -> service.replace(command(ownerId, SportType.BADMINTON, List.of(court1),
                LocalTime.of(5, 0), LocalTime.of(20, 0)), false))
                .isInstanceOf(ServiceException.class).hasMessageContaining("giờ mở cửa");
    }

    @Test
    void existingPlayerBookingBlocksTheHold() {
        when(timeSlotRepository.findByCourtsAndDateRangeForUpdate(any(), any(), any())).thenReturn(List.of(
                TimeSlot.builder().venueCourtId(court2).date(to).startTime(LocalTime.of(18, 0))
                        .endTime(LocalTime.of(19, 0)).status(TimeSlotStatus.BOOKED).build(),
                TimeSlot.builder().venueCourtId(court1).date(from).startTime(LocalTime.of(21, 0))
                        .endTime(LocalTime.of(22, 0)).status(TimeSlotStatus.BOOKED).build()));

        assertThatThrownBy(() -> service.replace(command(ownerId, SportType.BADMINTON, List.of(court1, court2),
                LocalTime.of(8, 0), LocalTime.of(20, 0)), false))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Sân 2 (1 lượt)")
                .hasMessageNotContaining("Sân 1");
        verify(courtHoldRepository, never()).saveAll(any());
    }

    @Test
    void otherTournamentOnSameCourtAndHoursBlocksTheHold() {
        when(courtHoldRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(CourtHold.builder()
                .tournamentId(UUID.randomUUID()).venueCourtId(court1).fromDate(to).toDate(to.plusDays(2))
                .dailyStart(LocalTime.of(18, 0)).dailyEnd(LocalTime.of(22, 0)).build()));

        assertThatThrownBy(() -> service.replace(command(ownerId, SportType.BADMINTON, List.of(court1),
                LocalTime.of(8, 0), LocalTime.of(20, 0)), false))
                .isInstanceOf(ServiceException.class).hasMessageContaining("giải khác");
    }

    @Test
    void replacingOwnHoldIsNotAConflict() throws ServiceException {
        when(courtHoldRepository.findOverlapping(any(), any(), any())).thenReturn(List.of(CourtHold.builder()
                .tournamentId(tournamentId).venueCourtId(court1).fromDate(from).toDate(to)
                .dailyStart(LocalTime.of(8, 0)).dailyEnd(LocalTime.of(20, 0)).build()));

        assertThat(service.replace(command(ownerId, SportType.BADMINTON, List.of(court1),
                LocalTime.of(9, 0), LocalTime.of(21, 0)), false)).hasSize(1);
    }

    @Test
    void dryRunValidatesWithoutHolding() throws ServiceException {
        assertThat(service.replace(command(ownerId, SportType.BADMINTON, List.of(court1),
                LocalTime.of(8, 0), LocalTime.of(20, 0)), true)).isEmpty();
        verify(courtHoldRepository, never()).saveAll(any());
        verify(courtHoldRepository, never()).deleteByTournamentId(any());
    }
}
