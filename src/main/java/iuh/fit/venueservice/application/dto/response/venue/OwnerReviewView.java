package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.AiVerdict;
import iuh.fit.venueservice.domain.enums.ReviewStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record OwnerReviewView(
        UUID reviewId,
        UUID venueId,
        String venueName,
        UUID venueCourtId,
        String courtName,
        UUID bookingId,
        String bookingCode,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer rating,
        String content,
        ReviewStatus status,
        LocalDateTime createdAt,
        AiVerdict ai
) {
}
