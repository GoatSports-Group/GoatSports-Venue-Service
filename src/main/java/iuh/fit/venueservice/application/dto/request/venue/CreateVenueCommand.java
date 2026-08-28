package iuh.fit.venueservice.application.dto.request.venue;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateVenueCommand(
        String name,
        String description,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean active,
        Double minPrice,
        Double maxPrice,
        String phone,
        String email,
        String address,
        String ward,
        String district,
        String city,
        Double latitude,
        Double longitude,
        List<String> imageUrls,
        List<String> amenities,
        UUID currentUserId
) {}
