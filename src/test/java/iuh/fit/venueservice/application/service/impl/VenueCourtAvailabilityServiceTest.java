package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.CourtAvailabilityStatus;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VenueCourtAvailabilityServiceTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void courtBecomesAvailableAtExactEndBoundary() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        VenueRepository venueRepository = mock(VenueRepository.class);
        VenueCourtRepository courtRepository = mock(VenueCourtRepository.class);
        TimeSlotRepository slotRepository = mock(TimeSlotRepository.class);
        Clock exactlyEight = Clock.fixed(Instant.parse("2026-09-01T01:00:00Z"), BUSINESS_ZONE);
        Venue venue = Venue.builder().venueId(venueId).ownerId(ownerId).build();
        VenueCourt court = VenueCourt.builder()
                .venueCourtId(courtId).venueId(venueId).active(true).build();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(courtRepository.findByVenueId(venueId)).thenReturn(List.of(court));
        when(slotRepository.findBlockingSlotsAt(
                Set.of(courtId), LocalDate.of(2026, 9, 1), LocalTime.of(8, 0)
        )).thenReturn(List.of());

        VenueCourtServiceImpl service = new VenueCourtServiceImpl(
                courtRepository, venueRepository, slotRepository, exactlyEight
        );

        assertThat(service.getVenueCourts(venueId, ownerId).getFirst().getAvailabilityStatus())
                .isEqualTo(CourtAvailabilityStatus.AVAILABLE);
        verify(slotRepository).findBlockingSlotsAt(
                Set.of(courtId), LocalDate.of(2026, 9, 1), LocalTime.of(8, 0)
        );
    }

    @Test
    void bookedSlotKeepsCourtOccupiedBeforeEndBoundary() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        VenueRepository venueRepository = mock(VenueRepository.class);
        VenueCourtRepository courtRepository = mock(VenueCourtRepository.class);
        TimeSlotRepository slotRepository = mock(TimeSlotRepository.class);
        Clock beforeEight = Clock.fixed(Instant.parse("2026-09-01T00:59:00Z"), BUSINESS_ZONE);
        Venue venue = Venue.builder().venueId(venueId).ownerId(ownerId).build();
        VenueCourt court = VenueCourt.builder()
                .venueCourtId(courtId).venueId(venueId).active(true).build();
        TimeSlot slot = TimeSlot.builder()
                .venueCourtId(courtId).date(LocalDate.of(2026, 9, 1))
                .startTime(LocalTime.of(7, 0)).endTime(LocalTime.of(8, 0))
                .status(TimeSlotStatus.BOOKED).build();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(courtRepository.findByVenueId(venueId)).thenReturn(List.of(court));
        when(slotRepository.findBlockingSlotsAt(
                Set.of(courtId), LocalDate.of(2026, 9, 1), LocalTime.of(7, 59)
        )).thenReturn(List.of(slot));

        VenueCourtServiceImpl service = new VenueCourtServiceImpl(
                courtRepository, venueRepository, slotRepository, beforeEight
        );
        VenueCourt result = service.getVenueCourts(venueId, ownerId).getFirst();

        assertThat(result.getAvailabilityStatus()).isEqualTo(CourtAvailabilityStatus.OCCUPIED);
        assertThat(result.getUnavailableUntil()).isEqualTo(LocalTime.of(8, 0));
    }
}
