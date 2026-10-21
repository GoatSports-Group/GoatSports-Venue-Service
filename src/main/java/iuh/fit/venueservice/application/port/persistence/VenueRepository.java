package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.request.venue.AdminVenueFilter;
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

    /** Chủ của ít nhất một cơ sở (report-service tạo báo cáo định kỳ cho từng người). */
    List<UUID> findOwnerIds();

    PageResult<Venue> search(SearchVenueCommand command);

    PageResult<Venue> findAll(AdminVenueFilter filter);

    boolean existsByOwnerId(UUID ownerId);
}
