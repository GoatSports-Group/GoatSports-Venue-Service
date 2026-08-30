package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.Booking;

import java.util.UUID;

public record OwnerBookingRecord(
        Booking booking,
        UUID venueId,
        String venueName,
        String courtName
) {
}
