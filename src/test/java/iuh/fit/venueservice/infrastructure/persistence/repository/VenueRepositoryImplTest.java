package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.infrastructure.persistence.entity.AddressEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.AddressEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        when(jpaRepository.findAll(ArgumentMatchers.<Specification<VenueEntity>>any()))
                .thenReturn(List.of(far, withoutCoordinates, near));
        when(venueEntityMapper.toDomain(near)).thenReturn(nearDomain);
        SearchVenueCommand command = new SearchVenueCommand(
                null, null, null, null,
                10.0, 106.0, 20.0,
                null, null, null, null,
                null, null, null,
                0, 10
        );

        PageResult<Venue> result = repository.search(command);

        assertThat(result.items()).containsExactly(nearDomain);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void minimumPriceFilterUsesTheSameMinimumPriceDisplayedOnVenueCards() {
        when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        SearchVenueCommand command = new SearchVenueCommand(
                null, null, null, null,
                null, null, null,
                null, null, null, null,
                500_000.0, null, null,
                0, 10
        );

        repository.search(command);

        ArgumentCaptor<Specification> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(jpaRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Root<VenueEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Boolean> activePath = mock(Path.class);
        Path<Double> minimumPricePath = mock(Path.class);
        when(root.<Boolean>get("active")).thenReturn(activePath);
        when(root.<Double>get("minPrice")).thenReturn(minimumPricePath);
        when(criteriaBuilder.isTrue(activePath)).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.greaterThanOrEqualTo(minimumPricePath, 500_000.0))
                .thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        Specification<VenueEntity> specification = specificationCaptor.getValue();
        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(minimumPricePath, 500_000.0);
    }

    @Test
    void resolvesLocalizedSportNameFromKeyword() {
        assertThat(VenueRepositoryImpl.resolveSportTypeKeyword("Bóng đá"))
                .contains(SportType.FOOTBALL);
        assertThat(VenueRepositoryImpl.resolveSportTypeKeyword("bong da"))
                .contains(SportType.FOOTBALL);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void areaFilterMatchesDistrictOrWardAndAppliesViewportBounds() /* tỉnh, địa chỉ: VenueKeywordSearchTest */ {
        when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        SearchVenueCommand command = new SearchVenueCommand(
                null, null, "Phường Ninh", null,
                null, null, null,
                13.0, 12.0, 110.0, 109.0,
                null, null, null,
                0, 10
        );

        repository.search(command);

        ArgumentCaptor<Specification> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(jpaRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Root<VenueEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Join<VenueEntity, AddressEntity> addressJoin = mock(Join.class);
        Path<Boolean> activePath = mock(Path.class);
        Path<String> districtPath = mock(Path.class);
        Path<String> wardPath = mock(Path.class);
        Path<Double> latitudePath = mock(Path.class);
        Path<Double> longitudePath = mock(Path.class);
        Expression<String> lowerDistrict = mock(Expression.class);
        Expression<String> lowerWard = mock(Expression.class);
        Predicate districtMatch = mock(Predicate.class);
        Predicate wardMatch = mock(Predicate.class);

        when(root.<Boolean>get("active")).thenReturn(activePath);
        when(root.join("address", JoinType.INNER)).thenReturn((Join) addressJoin);
        when(addressJoin.<String>get("district")).thenReturn(districtPath);
        when(addressJoin.<String>get("ward")).thenReturn(wardPath);
        when(addressJoin.<Double>get("latitude")).thenReturn(latitudePath);
        when(addressJoin.<Double>get("longitude")).thenReturn(longitudePath);
        when(criteriaBuilder.lower(districtPath)).thenReturn(lowerDistrict);
        when(criteriaBuilder.lower(wardPath)).thenReturn(lowerWard);
        Path<String> cityPath = mock(Path.class);
        Path<String> streetPath = mock(Path.class);
        when(addressJoin.<String>get("city")).thenReturn(cityPath);
        when(addressJoin.<String>get("address")).thenReturn(streetPath);
        when(criteriaBuilder.lower(cityPath)).thenReturn(mock(Expression.class));
        when(criteriaBuilder.lower(streetPath)).thenReturn(mock(Expression.class));
        when(criteriaBuilder.like(any(Expression.class), eq("%phường ninh%"))).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.isTrue(activePath)).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.like(lowerDistrict, "%phường ninh%")).thenReturn(districtMatch);
        when(criteriaBuilder.like(lowerWard, "%phường ninh%")).thenReturn(wardMatch);
        when(criteriaBuilder.or(any(Predicate[].class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.between(latitudePath, 12.0, 13.0)).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.between(longitudePath, 109.0, 110.0)).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        Specification<VenueEntity> specification = specificationCaptor.getValue();
        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(lowerDistrict, "%phường ninh%");
        verify(criteriaBuilder).like(lowerWard, "%phường ninh%");
        verify(criteriaBuilder).between(latitudePath, 12.0, 13.0);
        verify(criteriaBuilder).between(longitudePath, 109.0, 110.0);
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
