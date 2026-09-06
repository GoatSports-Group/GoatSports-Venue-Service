package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.persistence.entity.AddressEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.AddressEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueRepositoryImplTest {
    @Mock
    private JpaVenueRepository jpaRepository;
    @Mock
    private VenueEntityMapper venueEntityMapper;
    @Mock
    private AddressEntityMapper addressEntityMapper;

    private VenueRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new VenueRepositoryImpl(jpaRepository, venueEntityMapper, addressEntityMapper);
    }

    @Test
    void radiusSearchKeepsOnlyVenuesWithCoordinatesInsideRequestedDistance() {
        UUID nearId = UUID.randomUUID();
        VenueEntity near = venue(nearId, 10.05, 106.0);
        VenueEntity far = venue(UUID.randomUUID(), 11.0, 106.0);
        VenueEntity withoutCoordinates = venue(UUID.randomUUID(), null, null);
        Venue nearDomain = Venue.builder().venueId(nearId).build();
        when(jpaRepository.findAll(org.mockito.ArgumentMatchers.<Specification<VenueEntity>>any()))
                .thenReturn(List.of(far, withoutCoordinates, near));
        when(venueEntityMapper.toDomain(near)).thenReturn(nearDomain);
        SearchVenueCommand command = new SearchVenueCommand(
                null, null, null, null,
                10.0, 106.0, 20.0,
                null, null, null,
                0, 10
        );

        var result = repository.search(command);

        assertThat(result.items()).containsExactly(nearDomain);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    private VenueEntity venue(UUID venueId, Double latitude, Double longitude) {
        return VenueEntity.builder()
                .venueId(venueId)
                .active(true)
                .address(AddressEntity.builder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .build())
                .build();
    }
}
