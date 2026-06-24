package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;

import java.util.Optional;
import java.util.UUID;

public interface VenueOwnerProfileRepository {
    VenueOwnerProfile save(VenueOwnerProfile profile);
    Optional<VenueOwnerProfile> findById(UUID id);
    Optional<VenueOwnerProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
