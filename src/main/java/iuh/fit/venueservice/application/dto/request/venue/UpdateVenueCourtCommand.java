package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record UpdateVenueCourtCommand(
        UUID venueCourtId,
        String name,
        String sportType,
        Integer capacity,
        String surfaceType,
        Boolean active,
        UUID currentUserId
) {}
