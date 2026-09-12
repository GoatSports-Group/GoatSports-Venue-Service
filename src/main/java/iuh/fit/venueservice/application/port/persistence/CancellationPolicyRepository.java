package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.CancellationPolicy;

import java.util.Optional;
import java.util.UUID;

public interface CancellationPolicyRepository {
    Optional<CancellationPolicy> findByVenueId(UUID venueId);

    CancellationPolicy save(UUID venueId, CancellationPolicy policy);
}
