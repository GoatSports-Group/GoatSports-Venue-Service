package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record CreateVenueCourtCommand(
        UUID venueId,
        String sportType,
        String name,
        Integer capacity,
        String surfaceType,
        Boolean active,
        UUID currentUserId
) {}
