package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record CancelBookingCommand(
        UUID bookingId,
        UUID requestedByUserId,
        String reason
) {}
