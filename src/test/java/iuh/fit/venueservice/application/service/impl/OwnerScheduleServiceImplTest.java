package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.GenerateTimeSlotsCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpsertCourtPricingRuleCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.CourtHoldRepository;
import iuh.fit.venueservice.application.port.persistence.CourtPricingRuleRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.CourtHold;
import iuh.fit.venueservice.domain.entity.CourtPricingRule;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
class OwnerScheduleServiceImplTest {
    @Mock private CourtPricingRuleRepository pricingRuleRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private VenueCourtRepository venueCourtRepository;
    @Mock private VenueRepository venueRepository;
    @Mock(answer = Answers.CALLS_REAL_METHODS) private CourtHoldRepository courtHoldRepository;

    private OwnerScheduleServiceImpl service;
    private UUID ownerId;
    private UUID courtId;
    private Venue venue;

    @BeforeEach
    void setUp() {
        service = new OwnerScheduleServiceImpl(
                pricingRuleRepository, timeSlotRepository, venueCourtRepository, venueRepository, courtHoldRepository
        );
        ownerId = UUID.randomUUID();
        courtId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        venue = Venue.builder()
                .venueId(venueId).ownerId(ownerId)
                .minPrice(100_000.0).maxPrice(300_000.0)
                .openTime(LocalTime.of(6, 0)).closeTime(LocalTime.of(22, 0)).build();
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build()
        ));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
    }

    @Test
    void createsRuleWithinOpeningHours() throws Exception {
        UpsertCourtPricingRuleCommand command = ruleCommand(null, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(pricingRuleRepository.findByCourtId(courtId)).thenReturn(List.of());
        when(pricingRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CourtPricingRule result = service.createPricingRule(command);

        assertThat(result.getCourtId()).isEqualTo(courtId);
        assertThat(result.getPricePerHour()).isEqualTo(180_000.0);
        verify(pricingRuleRepository).save(any(CourtPricingRule.class));
    }

    @Test
    void rejectsOverlappingRuleInSameEffectiveWindow() {
        CourtPricingRule existing = rule(UUID.randomUUID(), LocalTime.of(9, 0), LocalTime.of(13, 0));
        when(pricingRuleRepository.findByCourtId(courtId)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.createPricingRule(
                ruleCommand(null, LocalTime.of(8, 0), LocalTime.of(10, 0))
        )).isInstanceOf(ServiceException.class).hasMessageContaining("chồng lấn");
        verify(pricingRuleRepository, never()).save(any());
    }

    @Test
    void rejectsContainedTimeAndEffectiveWindowForSameWeekday() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        CourtPricingRule existing = rule(UUID.randomUUID(), LocalTime.of(7, 0), LocalTime.of(21, 0));
        existing.setEffectiveFrom(monday);
        existing.setEffectiveTo(monday.plusDays(6));
        when(pricingRuleRepository.findByCourtId(courtId)).thenReturn(List.of(existing));

        UpsertCourtPricingRuleCommand candidate = new UpsertCourtPricingRuleCommand(
                null, courtId, DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(9, 0),
                200_000.0, 200_000.0, monday, monday.plusDays(1), ownerId
        );

        assertThatThrownBy(() -> service.createPricingRule(candidate))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("07:00-21:00")
                .hasMessageContaining(monday.toString());
        verify(pricingRuleRepository, never()).save(any());
    }

    @Test
    void allowsSameWeekdayAndTimeWhenEffectiveWindowsDoNotOverlap() throws Exception {
        LocalDate firstMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate secondMonday = firstMonday.plusWeeks(1);
        CourtPricingRule existing = rule(UUID.randomUUID(), LocalTime.of(7, 0), LocalTime.of(9, 0));
        existing.setEffectiveFrom(firstMonday);
        existing.setEffectiveTo(firstMonday.plusDays(6));
        when(pricingRuleRepository.findByCourtId(courtId)).thenReturn(List.of(existing));
        when(pricingRuleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpsertCourtPricingRuleCommand candidate = new UpsertCourtPricingRuleCommand(
                null, courtId, DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(9, 0),
                200_000.0, 200_000.0, secondMonday, secondMonday.plusDays(6), ownerId
        );

        CourtPricingRule result = service.createPricingRule(candidate);

        assertThat(result.getEffectiveFrom()).isEqualTo(secondMonday);
        verify(pricingRuleRepository).save(any(CourtPricingRule.class));
    }

    @Test
    void rejectsCrossOwnerAccess() {
        venue.setOwnerId(UUID.randomUUID());

        assertThatThrownBy(() -> service.getPricingRules(courtId, ownerId))
                .isInstanceOf(OwnershipException.class);
    }

    @Test
    void rejectsRulePriceOutsideVenueRange() {
        UpsertCourtPricingRuleCommand command = new UpsertCourtPricingRuleCommand(
                null, courtId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0),
                350_000.0, 350_000.0,
                // Tu ngay mai: quy tac thu Hai 8:00 "bat dau hom nay" se bi coi la khung da qua neu test chay vao thu Hai.
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), ownerId
        );

        assertThatThrownBy(() -> service.createPricingRule(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("khoảng giá min-max");
        verify(pricingRuleRepository, never()).save(any());
    }

    @Test
    void rejectsCreatingPricingRuleInThePast() {
        UpsertCourtPricingRuleCommand command = new UpsertCourtPricingRuleCommand(
                null, courtId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0),
                180_000.0, 180_000.0,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(7), ownerId
        );

        assertThatThrownBy(() -> service.createPricingRule(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("thời điểm hiện tại");
        verify(pricingRuleRepository, never()).save(any());
    }

    @Test
    void rejectsPricingRuleWhenSelectedWeekdayIsOutsideEffectiveRange() {
        LocalDate friday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        UpsertCourtPricingRuleCommand command = new UpsertCourtPricingRuleCommand(
                null, courtId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0),
                180_000.0, 180_000.0, friday, friday.plusDays(2), ownerId
        );

        assertThatThrownBy(() -> service.createPricingRule(command))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("thứ đã chọn");
        verify(pricingRuleRepository, never()).save(any());
    }

    @Test
    void generatesAvailableSlotsWithPricingSnapshotAndSkipsExactDuplicates() throws Exception {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        CourtPricingRule rule = rule(UUID.randomUUID(), LocalTime.of(8, 0), LocalTime.of(10, 0));
        rule.setEffectiveFrom(monday.minusDays(1));
        rule.setEffectiveTo(monday.plusDays(1));
        TimeSlot existingSlot = TimeSlot.builder()
                .timeSlotId(UUID.randomUUID()).venueCourtId(courtId).date(monday)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .status(TimeSlotStatus.AVAILABLE).pricePerHour(180_000.0).build();
        when(pricingRuleRepository.findByCourtId(courtId)).thenReturn(List.of(rule));
        when(timeSlotRepository.findByVenueCourtIdAndDateBetween(courtId, monday, monday))
                .thenReturn(new ArrayList<>(List.of(existingSlot)));
        when(timeSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeSlot> result = service.generateTimeSlots(
                new GenerateTimeSlotsCommand(
                        courtId, monday, monday, List.of(rule.getPricingRuleId()), 60, ownerId
                )
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(1).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(1).getPricePerHour()).isEqualTo(180_000.0);
        assertThat(result.get(1).getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
    }

    @Test
    void synchronizesRemainingSlotsWhenAnExistingBookedSlotPartiallyOverlaps() throws Exception {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        CourtPricingRule rule = rule(UUID.randomUUID(), LocalTime.of(8, 0), LocalTime.of(10, 0));
        rule.setEffectiveFrom(monday.minusDays(1));
        rule.setEffectiveTo(monday.plusDays(1));
        TimeSlot bookedSlot = TimeSlot.builder()
                .timeSlotId(UUID.randomUUID()).venueCourtId(courtId).date(monday)
                .startTime(LocalTime.of(8, 10)).endTime(LocalTime.of(8, 20))
                .status(TimeSlotStatus.BOOKED).pricePerHour(180_000.0).build();
        when(pricingRuleRepository.findByCourtId(courtId)).thenReturn(List.of(rule));
        when(timeSlotRepository.findByVenueCourtIdAndDateBetween(courtId, monday, monday))
                .thenReturn(new ArrayList<>(List.of(bookedSlot)));
        when(timeSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeSlot> result = service.generateTimeSlots(
                new GenerateTimeSlotsCommand(
                        courtId, monday, monday, List.of(rule.getPricingRuleId()), 60, ownerId
                )
        );

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(slot -> {
            assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(slot.getEndTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
        });
    }

    @Test
    void generatesSlotsOnlyFromSelectedPricingRules() throws Exception {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        CourtPricingRule morningRule = rule(UUID.randomUUID(), LocalTime.of(8, 0), LocalTime.of(9, 0));
        CourtPricingRule eveningRule = rule(UUID.randomUUID(), LocalTime.of(18, 0), LocalTime.of(19, 0));
        morningRule.setEffectiveFrom(monday);
        morningRule.setEffectiveTo(monday);
        eveningRule.setEffectiveFrom(monday);
        eveningRule.setEffectiveTo(monday);
        when(pricingRuleRepository.findByCourtId(courtId)).thenReturn(List.of(morningRule, eveningRule));
        when(timeSlotRepository.findByVenueCourtIdAndDateBetween(courtId, monday, monday))
                .thenReturn(new ArrayList<>());
        when(timeSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeSlot> result = service.generateTimeSlots(new GenerateTimeSlotsCommand(
                courtId, monday, monday, List.of(eveningRule.getPricingRuleId()), 60, ownerId
        ));

        assertThat(result).singleElement().satisfies(slot -> {
            assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(slot.getEndTime()).isEqualTo(LocalTime.of(19, 0));
        });
    }

    @Test
    void preventsOwnerFromChangingBookedSlot() {
        UUID slotId = UUID.randomUUID();
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(TimeSlot.builder()
                .timeSlotId(slotId).venueCourtId(courtId).status(TimeSlotStatus.BOOKED).build()));

        assertThatThrownBy(() -> service.updateTimeSlotStatus(
                slotId, TimeSlotStatus.MAINTENANCE, ownerId
        )).isInstanceOf(ServiceException.class).hasMessageContaining("đã đặt");
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void preventsDeletingSlotHeldByTournament() {
        UUID slotId = UUID.randomUUID();
        LocalDate day = LocalDate.now().plusDays(1);
        when(timeSlotRepository.findById(slotId)).thenReturn(Optional.of(TimeSlot.builder()
                .timeSlotId(slotId).venueCourtId(courtId).date(day)
                .startTime(LocalTime.of(16, 0)).endTime(LocalTime.of(17, 0)).status(TimeSlotStatus.AVAILABLE).build()));
        when(courtHoldRepository.findOverlapping(List.of(courtId), day, day)).thenReturn(List.of(CourtHold.builder()
                .tournamentId(UUID.randomUUID()).venueCourtId(courtId).fromDate(day).toDate(day)
                .dailyStart(LocalTime.of(6, 0)).dailyEnd(LocalTime.of(23, 0)).build()));

        assertThatThrownBy(() -> service.deleteTimeSlot(slotId, ownerId))
                .isInstanceOf(ServiceException.class).hasMessageContaining("giải đấu");
        verify(timeSlotRepository, never()).deleteById(any());
    }

    private UpsertCourtPricingRuleCommand ruleCommand(
            UUID ruleId, LocalTime startTime, LocalTime endTime
    ) {
        return new UpsertCourtPricingRuleCommand(
                ruleId, courtId, DayOfWeek.MONDAY, startTime, endTime,
                180_000.0, 150_000.0,
                // Tu ngay mai: quy tac thu Hai 8:00 "bat dau hom nay" se bi coi la khung da qua neu test chay vao thu Hai.
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(3), ownerId
        );
    }

    private CourtPricingRule rule(UUID ruleId, LocalTime startTime, LocalTime endTime) {
        return CourtPricingRule.builder()
                .pricingRuleId(ruleId).courtId(courtId).dayOfWeek(DayOfWeek.MONDAY)
                .startTime(startTime).endTime(endTime).pricePerHour(180_000.0)
                .basePricePerHour(150_000.0).effectiveFrom(LocalDate.now().plusDays(1))
                .effectiveTo(LocalDate.now().plusMonths(3)).build();
    }
}
