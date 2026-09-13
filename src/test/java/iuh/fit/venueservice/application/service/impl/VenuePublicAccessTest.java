package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenuePublicAccessTest {
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private VenueCourtRepository venueCourtRepository;

    private VenueSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VenueSearchServiceImpl(venueRepository, venueCourtRepository);
    }

    @Test
    void publicDetailDoesNotExposeInactiveVenue() {
        UUID venueId = UUID.randomUUID();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).active(false).build()
        ));

        assertThatThrownBy(() -> service.getVenueDetails(venueId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publicCourtListContainsOnlyActiveCourts() throws Exception {
        UUID venueId = UUID.randomUUID();
        VenueCourt active = VenueCourt.builder()
                .venueCourtId(UUID.randomUUID()).venueId(venueId).active(true).build();
        VenueCourt inactive = VenueCourt.builder()
                .venueCourtId(UUID.randomUUID()).venueId(venueId).active(false).build();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).active(true).build()
        ));
        when(venueCourtRepository.findByVenueId(venueId)).thenReturn(List.of(active, inactive));

        assertThat(service.getVenueCourts(venueId)).containsExactly(active);
    }

    @Test
    void publicCourtDetailRequiresActiveParentVenue() {
        UUID venueId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(venueId).active(true).build()
        ));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).active(false).build()
        ));

        assertThatThrownBy(() -> service.getVenueCourt(courtId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchDoesNotInventRatingForVenueWithoutReviews() {
        UUID venueId = UUID.randomUUID();
        SearchVenueCommand command = new SearchVenueCommand(
                null, null, null, null,
                null, null, null,
                null, null, null, null,
                null, null, null,
                0, 12
        );
        Venue venue = Venue.builder()
                .venueId(venueId)
                .active(true)
                .averageRating(null)
                .totalReviews(null)
                .build();
        when(venueRepository.search(command))
                .thenReturn(new PageResult<>(List.of(venue), 1, 0, 12, 1));
        when(venueCourtRepository.findByVenueId(venueId)).thenReturn(List.of());

        var result = service.searchVenues(command);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().averageRating()).isZero();
        assertThat(result.items().getFirst().totalReviews()).isZero();
    }
}
