package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.Venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository {
    Venue save(Venue venue);

    Optional<Venue> findById(UUID id);

    List<Venue> findByUserId(UUID userId);

    PageResult<Venue> search(SearchVenueCommand command);

    PageResult<Venue> findAll(int page, int size);
}
