package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDateTime;
import java.util.UUID;

public record VenueResponseCommand(
        UUID venueId,
        String name,
        String description,
        LocalDateTime openTime,
        LocalDateTime closeTime,
        Boolean active,
        Double minPrice,
        Double maxPrice,
        Double averageRating,
        Integer totalReviews,
        String phone,
        String email
) {}
