package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueCourtEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueCourtEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenueCourtRepositoryImpl implements VenueCourtRepository {
    private final JpaVenueCourtRepository jpaRepository;
    private final VenueCourtEntityMapper venueCourtEntityMapper;
    private final VenueEntityMapper venueEntityMapper;

    @Override
    public VenueCourt save(VenueCourt venueCourt, Venue venue) {
        VenueEntity venueEntity = venueEntityMapper.createEntity(venue);
        VenueCourtEntity entity = venueCourtEntityMapper.createEntity(venueCourt, venueEntity);
        VenueCourtEntity saved = jpaRepository.save(entity);
        return venueCourtEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<VenueCourt> findById(UUID id) {
        return jpaRepository.findById(id).map(venueCourtEntityMapper::toDomain);
    }
}
