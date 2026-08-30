package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record CreatePlayerReviewCommand(
        UUID playerId,
        UUID bookingId,
        Integer rating,
        String content
) {
}
