package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.domain.enums.CourtAvailabilityStatus;

import java.time.LocalTime;
import java.util.UUID;

public record VenueCourtResponseCommand(
        UUID venueCourtId,
        UUID venueId,
        SportType sportType,
        String name,
        Integer capacity,
        String surfaceType,
        Boolean active,
        CourtAvailabilityStatus availabilityStatus,
        LocalTime unavailableUntil
) {}
