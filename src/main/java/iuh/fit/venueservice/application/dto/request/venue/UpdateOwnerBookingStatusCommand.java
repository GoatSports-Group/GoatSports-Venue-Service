package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.util.UUID;

public record UpdateOwnerBookingStatusCommand(
        UUID bookingId,
        BookingStatus status,
        UUID ownerId
) {
}
