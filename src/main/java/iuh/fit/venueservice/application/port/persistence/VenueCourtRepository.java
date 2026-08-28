package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueCourtRepository {
    VenueCourt save(VenueCourt venueCourt, Venue venue);

    Optional<VenueCourt> findById(UUID id);

    List<VenueCourt> findByVenueId(UUID venueId);
}
