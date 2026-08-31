package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record CreateOwnerBookingPaymentCommand(
        UUID bookingId,
        UUID ownerId,
        OwnerBookingPaymentMethod method
) {
}
