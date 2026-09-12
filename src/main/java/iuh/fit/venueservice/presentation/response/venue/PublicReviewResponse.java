package iuh.fit.venueservice.presentation.response.venue;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublicReviewResponse(
        UUID reviewId,
        Integer rating,
        String content,
        LocalDateTime createdAt
) {
}
