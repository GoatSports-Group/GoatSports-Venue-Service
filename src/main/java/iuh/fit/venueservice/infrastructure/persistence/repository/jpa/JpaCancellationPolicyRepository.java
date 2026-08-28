package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.CancellationPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCancellationPolicyRepository extends JpaRepository<CancellationPolicyEntity, UUID> {
    Optional<CancellationPolicyEntity> findByVenue_VenueId(UUID venueId);
}
