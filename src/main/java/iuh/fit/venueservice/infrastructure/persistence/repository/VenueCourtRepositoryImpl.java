package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueCourtEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueCourtMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenueCourtRepositoryImpl implements VenueCourtRepository {
    private final JpaVenueCourtRepository jpaRepository;

    @Override
    public VenueCourt save(VenueCourt venueCourt, Venue venue) {
        VenueEntity venueEntity = VenueMapper.toEntity(venue);
        VenueCourtEntity entity = VenueCourtMapper.toEntity(venueCourt, venueEntity);
        VenueCourtEntity saved = jpaRepository.save(entity);
        return VenueCourtMapper.toDomain(saved);
    }

    @Override
    public Optional<VenueCourt> findById(UUID id) {
        return jpaRepository.findById(id).map(VenueCourtMapper::toDomain);
    }
}
