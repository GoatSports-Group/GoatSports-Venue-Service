package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueCourtEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueCourtEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenueCourtRepositoryImpl implements VenueCourtRepository {
    private final JpaVenueCourtRepository jpaRepository;
    private final VenueCourtEntityMapper venueCourtEntityMapper;
    private final JpaVenueRepository jpaVenueRepository;

    @Override
    public VenueCourt save(VenueCourt venueCourt, Venue venue) {
        VenueCourtEntity entity;
        if (venueCourt.getVenueCourtId() == null) {
            entity = venueCourtEntityMapper.createEntity(
                    venueCourt, jpaVenueRepository.getReferenceById(venue.getVenueId())
            );
        } else {
            entity = jpaRepository.findById(venueCourt.getVenueCourtId())
                    .orElseGet(() -> venueCourtEntityMapper.createEntity(
                            venueCourt, jpaVenueRepository.getReferenceById(venue.getVenueId())
                    ));
            venueCourtEntityMapper.updateEntity(venueCourt, entity);
        }
        VenueCourtEntity saved = jpaRepository.save(entity);
        return venueCourtEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<VenueCourt> findById(UUID id) {
        return jpaRepository.findById(id).map(venueCourtEntityMapper::toDomain);
    }

    @Override
    public java.util.List<VenueCourt> findByVenueId(UUID venueId) {
        return jpaRepository.findByVenue_VenueId(venueId).stream()
                .map(venueCourtEntityMapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
