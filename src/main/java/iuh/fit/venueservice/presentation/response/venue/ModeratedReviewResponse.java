package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.domain.enums.ReviewStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ModeratedReviewResponse(
        UUID reviewId,
        UUID venueId,
        Integer rating,
        ReviewStatus status
) {
}
