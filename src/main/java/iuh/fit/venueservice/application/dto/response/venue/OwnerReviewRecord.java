package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.Review;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record OwnerReviewRecord(
        Review review,
        String venueName,
        UUID venueCourtId,
        String courtName,
        String bookingCode,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
