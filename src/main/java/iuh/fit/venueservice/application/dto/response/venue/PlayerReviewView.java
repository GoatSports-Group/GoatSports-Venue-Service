package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlayerReviewView(
        UUID reviewId,
        UUID venueId,
        UUID bookingId,
        Integer rating,
        String content,
        ReviewStatus status,
        LocalDateTime createdAt
) {
}
