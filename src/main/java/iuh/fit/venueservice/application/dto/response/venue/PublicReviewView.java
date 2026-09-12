package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublicReviewView(
        UUID reviewId,
        Integer rating,
        String content,
        LocalDateTime createdAt
) {
}
