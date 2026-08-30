package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record CreateWalkInBookingCommand(
        UUID ownerId,
        UUID venueCourtId,
        UUID timeSlotId,
        String customerName,
        String customerPhone
) {
}
