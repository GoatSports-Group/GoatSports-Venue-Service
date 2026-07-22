package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.VenueOwnerProfileRepository;
import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueOwnerProfileEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueOwnerProfileEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaOwnerApplicationRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueOwnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VenueOwnerProfileRepositoryImpl implements VenueOwnerProfileRepository {
    private final JpaVenueOwnerProfileRepository jpaRepository;
    private final JpaOwnerApplicationRepository jpaOwnerApplicationRepository;
    private final VenueOwnerProfileEntityMapper venueOwnerProfileEntityMapper;

    @Override
    public VenueOwnerProfile save(VenueOwnerProfile profile, UUID applicationId) throws ServiceException {
        OwnerApplicationEntity ownerAppEntity = jpaOwnerApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại."));

        VenueOwnerProfileEntity entity;
        if (profile != null && profile.getVenueOwnerProfileId() != null) {
            entity = jpaRepository.findById(profile.getVenueOwnerProfileId())
                    .map(existing -> {
                        venueOwnerProfileEntityMapper.updateEntity(profile, existing);
                        return existing;
                    })
                    .orElseThrow(() -> new ServiceException("Hồ sơ chủ sân không tồn tại."));
        } else {
            entity = venueOwnerProfileEntityMapper.createEntity(profile, ownerAppEntity);
        }

        VenueOwnerProfileEntity saved = jpaRepository.save(entity);

        return venueOwnerProfileEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<VenueOwnerProfile> findById(UUID id) {
        return jpaRepository.findById(id).map(venueOwnerProfileEntityMapper::toDomain);
    }

    @Override
    public Optional<VenueOwnerProfile> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(venueOwnerProfileEntityMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsByUserId(userId);
    }
}
