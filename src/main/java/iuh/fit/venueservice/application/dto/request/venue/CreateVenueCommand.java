package iuh.fit.venueservice.application.dto.request.venue;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateVenueCommand(
        String name,
        String description,
        LocalDateTime openTime,
        LocalDateTime closeTime,
        Boolean active,
        Double minPrice,
        Double maxPrice,
        String phone,
        String email,
        UUID currentUserId
) {}
