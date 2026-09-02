package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VenueFacilityLayoutServiceTest {
    private final VenueRepository venueRepository = mock(VenueRepository.class);
    private final StoragePort storagePort = mock(StoragePort.class);
    private VenueServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VenueServiceImpl(venueRepository, storagePort);
    }

    @Test
    void savesLayoutOnOwnedVenueAndUsesServerVenueAndTimestamp() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Venue venue = Venue.builder().venueId(venueId).ownerId(ownerId).build();
        VenueFacilityLayout submitted = layout(UUID.randomUUID());
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueRepository.save(venue)).thenReturn(venue);

        VenueFacilityLayout saved = service.updateFacilityLayout(venueId, submitted, ownerId);

        assertThat(saved.venueId()).isEqualTo(venueId);
        assertThat(saved.version()).isEqualTo(1);
        assertThat(saved.updatedAt()).isNotNull();
        assertThat(venue.getFacilityLayout()).isEqualTo(saved);
        verify(venueRepository).save(venue);
    }

    @Test
    void rejectsLayoutAccessForAnotherOwner() {
        UUID venueId = UUID.randomUUID();
        Venue venue = Venue.builder().venueId(venueId).ownerId(UUID.randomUUID()).build();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> service.updateFacilityLayout(venueId, layout(venueId), UUID.randomUUID()))
                .isInstanceOf(OwnershipException.class);
        verify(venueRepository, never()).save(any(Venue.class));
    }

    private VenueFacilityLayout layout(UUID venueId) {
        return new VenueFacilityLayout(
                1,
                venueId,
                List.of(new VenueFacilityLayout.Item(
                        "custom:medical", "CUSTOM", null, "Phòng y tế",
                        20, 40, 160, 80, 0, "zone-a", "users"
                )),
                List.of(new VenueFacilityLayout.Zone("zone-a", "KHU A", 0, 0, 500, 400)),
                null
        );
    }
}
