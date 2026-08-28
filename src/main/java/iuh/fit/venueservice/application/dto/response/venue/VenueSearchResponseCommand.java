package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record VenueSearchResponseCommand(
        UUID venueId,
        String name,
        String description,
        LocalTime openTime,
        LocalTime closeTime,
        Double minPrice,
        Double maxPrice,
        Double averageRating,
        Integer totalReviews,
        String phone,
        String address,
        String district,
        String city,
        Double latitude,
        Double longitude,
        List<String> imageUrls,
        List<String> amenities,
        List<String> sportTypes,
        Integer totalCourts,
        Double distanceKm
) {}
