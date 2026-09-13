package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.SportType;

public record SearchVenueCommand(
        String keyword,
        SportType sportType,
        String district,
        String city,
        Double latitude,
        Double longitude,
        Double radiusKm,
        Double northLatitude,
        Double southLatitude,
        Double eastLongitude,
        Double westLongitude,
        Double minPrice,
        Double maxPrice,
        Double minRating,
        int page,
        int size
) {}
