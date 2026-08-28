package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalTime;
import java.util.UUID;

public record VenueResponseCommand(
        UUID venueId,
        String name,
        String description,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean active,
        Double minPrice,
        Double maxPrice,
        Double averageRating,
        Integer totalReviews,
        String phone,
        String email
) {}
