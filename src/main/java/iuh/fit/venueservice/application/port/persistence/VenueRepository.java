package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.Venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository {
    Venue save(Venue venue);
    Optional<Venue> findById(UUID id);
    List<Venue> findByUserId(UUID userId);
}
