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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Optional<VenueCourt> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaRepository.findByVenueCourtIdAndDeletedAtIsNull(id).map(venueCourtEntityMapper::toDomain);
    }

    @Override
    public List<VenueCourt> findByVenueId(UUID venueId) {
        return jpaRepository.findByVenue_VenueIdAndDeletedAtIsNull(venueId).stream()
                .map(venueCourtEntityMapper::toDomain)
                .collect(Collectors.toList());
    }
}
