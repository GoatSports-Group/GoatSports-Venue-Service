package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.SportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class VenueOwnerAuthorizationTest {
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private VenueCourtRepository venueCourtRepository;

    private VenueServiceImpl venueService;
    private VenueCourtServiceImpl venueCourtService;
    private UUID venueId;
    private UUID ownerId;
    private UUID otherOwnerId;
    private Venue venue;

    @BeforeEach
    void setUp() {
        venueService = new VenueServiceImpl(venueRepository);
        venueCourtService = new VenueCourtServiceImpl(venueCourtRepository, venueRepository);
        venueId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        otherOwnerId = UUID.randomUUID();
        venue = Venue.builder()
                .venueId(venueId)
                .ownerId(ownerId)
                .name("Goat Arena")
                .address(Address.builder().address("1 Goat Street").city("Hồ Chí Minh").build())
                .build();
    }

    @Test
    void ownerCannotReadOrUpdateAnotherOwnersVenue() {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> venueService.getOwnedVenue(venueId, otherOwnerId))
                .isInstanceOf(OwnershipException.class);
        assertThatThrownBy(() -> venueService.updateVenue(updateVenueCommand(otherOwnerId)))
                .isInstanceOf(OwnershipException.class);

        verify(venueRepository, never()).save(any(Venue.class));
    }

    @Test
    void ownerCanUpdateOnlyDiagramVenueFieldsAndKeepsOwnership() throws Exception {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Venue updated = venueService.updateVenue(updateVenueCommand(ownerId));

        assertThat(updated.getOwnerId()).isEqualTo(ownerId);
        assertThat(updated.getVenueId()).isEqualTo(venueId);
        assertThat(updated.getName()).isEqualTo("Goat Sports Center");
        assertThat(updated.getAmenities()).containsExactly("Bãi giữ xe", "Wifi");
        assertThat(updated.getAddress().getDistrict()).isEqualTo("Quận 1");
    }

    @Test
    void ownerCannotListCourtsFromAnotherOwnersVenue() {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> venueCourtService.getVenueCourts(venueId, otherOwnerId))
                .isInstanceOf(OwnershipException.class);

        verify(venueCourtRepository, never()).findByVenueId(venueId);
    }

    @Test
    void ownerCannotReadUpdateOrToggleAnotherOwnersCourt() {
        UUID courtId = UUID.randomUUID();
        VenueCourt court = VenueCourt.builder()
                .venueCourtId(courtId)
                .venueId(venueId)
                .name("Sân 01")
                .active(true)
                .build();
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(court));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> venueCourtService.getVenueCourt(courtId, otherOwnerId))
                .isInstanceOf(OwnershipException.class);
        assertThatThrownBy(() -> venueCourtService.updateVenueCourt(new UpdateVenueCourtCommand(
                courtId, "Sân 02", "BADMINTON", 4, "Thảm PVC", true, otherOwnerId
        ))).isInstanceOf(OwnershipException.class);
        assertThatThrownBy(() -> venueCourtService.updateActive(courtId, false, otherOwnerId))
                .isInstanceOf(OwnershipException.class);

        verify(venueCourtRepository, never()).save(any(VenueCourt.class), any(Venue.class));
    }

    private UpdateVenueCommand updateVenueCommand(UUID currentUserId) {
        return new UpdateVenueCommand(
                venueId,
                "Goat Sports Center",
                "Cơ sở thể thao đa môn",
                LocalTime.of(6, 0),
                LocalTime.of(22, 0),
                true,
                100_000D,
                300_000D,
                "0909000000",
                "owner@goatsports.test",
                "1 Goat Street",
                "Bến Nghé",
                "Quận 1",
                "Hồ Chí Minh",
                10.77,
                106.70,
                List.of("https://example.test/venue.jpg"),
                List.of("Bãi giữ xe", "Wifi", "Wifi"),
                currentUserId
        );
    }
}
