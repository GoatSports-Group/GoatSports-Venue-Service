package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record OwnerCheckInLookupCommand(
        UUID ownerId,
        UUID bookingId,
        String qrCode,
        String bookingCode
) {
}
