package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.VenueOwnerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaVenueOwnerProfileRepository extends JpaRepository<VenueOwnerProfileEntity, UUID>,
        JpaSpecificationExecutor<VenueOwnerProfileEntity>
{
    Optional<VenueOwnerProfileEntity> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
