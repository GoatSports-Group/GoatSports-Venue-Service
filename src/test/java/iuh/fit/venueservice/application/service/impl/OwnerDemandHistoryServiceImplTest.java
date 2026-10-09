package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.DemandBookingRecord;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerDemandHistoryService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnerDemandHistoryServiceImplTest {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final VenueCourtRepository courts = mock(VenueCourtRepository.class);
    private final UUID ownerId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    // 10/10/2026 12:20 giờ Việt Nam -> cửa sổ 24 giờ kết thúc lúc 12:00.
    private final Clock clock = Clock.fixed(LocalDateTime.of(2026, 10, 10, 12, 20).atZone(VN).toInstant(), VN);
    private final OwnerDemandHistoryServiceImpl service =
            new OwnerDemandHistoryServiceImpl(bookings, venues, courts, clock);

    private void venueWithTwoCourts(LocalDateTime createdAt) {
        Venue venue = new Venue();
        venue.setVenueId(venueId);
        venue.setOwnerId(ownerId);
        venue.setName("GOAT");
        venue.setCreatedAt(createdAt);
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        VenueCourt active = new VenueCourt();
        active.setActive(true);
        VenueCourt paused = new VenueCourt();
        paused.setActive(false);
        when(courts.findByVenueId(venueId)).thenReturn(List.of(active, new VenueCourt(), paused));
    }

    @Test
    void occupancyCountsCourtHoursOverActiveCourtsIncludingPartialAndOvernightBookings() throws ServiceException {
        venueWithTwoCourts(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(bookings.findVenueDemandBookings(eq(venueId), any(), any(), any())).thenReturn(List.of(
                // 9/10 18:00-20:00 trên một sân -> 0,5 ở hai giờ 18 và 19.
                new DemandBookingRecord(LocalDate.of(2026, 10, 9), LocalTime.of(18, 0), LocalTime.of(20, 0)),
                // Cùng giờ 18:30-19:00 trên sân thứ hai -> giờ 18 thêm 0,25.
                new DemandBookingRecord(LocalDate.of(2026, 10, 9), LocalTime.of(18, 30), LocalTime.of(19, 0)),
                // 9/10 23:00 -> 10/10 01:00 (qua nửa đêm): giờ 23 và 0 mỗi giờ 0,5.
                new DemandBookingRecord(LocalDate.of(2026, 10, 9), LocalTime.of(23, 0), LocalTime.of(1, 0))));

        OwnerDemandHistoryService.DemandHistory history = service.hourlyOccupancy(ownerId, venueId, 24);

        assertThat(history.start()).isEqualTo(LocalDateTime.of(2026, 10, 9, 12, 0));
        assertThat(history.courtCount()).isEqualTo(2);
        List<Double> occupancy = history.occupancy();
        assertThat(occupancy).hasSize(24);
        assertThat(occupancy.get(6)).isEqualTo(0.75);   // 18:00
        assertThat(occupancy.get(7)).isEqualTo(0.5);    // 19:00
        assertThat(occupancy.get(8)).isEqualTo(0.0);    // 20:00
        assertThat(occupancy.get(11)).isEqualTo(0.5);   // 23:00
        assertThat(occupancy.get(12)).isEqualTo(0.5);   // 00:00 hôm sau
    }

    @Test
    void hoursBeforeTheVenueExistedAreUnknownNotZero() throws ServiceException {
        venueWithTwoCourts(LocalDateTime.of(2026, 10, 10, 2, 30));
        when(bookings.findVenueDemandBookings(eq(venueId), any(), any(), any())).thenReturn(List.of());

        List<Double> occupancy = service.hourlyOccupancy(ownerId, venueId, 24).occupancy();

        assertThat(occupancy.get(13)).isNull();          // 01:00, trước khi tạo cơ sở
        assertThat(occupancy.get(14)).isEqualTo(0.0);    // 02:00, giờ tạo cơ sở
    }

    @Test
    void anotherOwnersVenueIsRejected() {
        venueWithTwoCourts(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThatThrownBy(() -> service.hourlyOccupancy(UUID.randomUUID(), venueId, 24))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.hourlyOccupancy(ownerId, venueId, 4096))
                .isInstanceOf(ServiceException.class);
    }
}
