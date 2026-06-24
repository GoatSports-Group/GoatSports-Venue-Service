package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.enums.SportType;

import java.util.UUID;

public record VenueCourtResponseCommand(
        UUID venueCourtId,
        SportType sportType,
        String name,
        Integer capacity,
        Double pricePerHour,
        Boolean indoor,
        Boolean active
) {}
