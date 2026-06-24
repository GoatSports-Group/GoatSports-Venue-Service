package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.VenueOwnerProfileRepository;
import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueOwnerProfileEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueOwnerProfileMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueOwnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenueOwnerProfileRepositoryImpl implements VenueOwnerProfileRepository {
    private final JpaVenueOwnerProfileRepository jpaRepository;

    @Override
    public VenueOwnerProfile save(VenueOwnerProfile profile) {
        VenueOwnerProfileEntity entity = VenueOwnerProfileMapper.toEntity(profile);
        VenueOwnerProfileEntity saved = jpaRepository.save(entity);
        return VenueOwnerProfileMapper.toDomain(saved);
    }

    @Override
    public Optional<VenueOwnerProfile> findById(UUID id) {
        return jpaRepository.findById(id).map(VenueOwnerProfileMapper::toDomain);
    }

    @Override
    public Optional<VenueOwnerProfile> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(VenueOwnerProfileMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsByUserId(userId);
    }
}
