package iuh.fit.venueservice.application.dto.request.venue;

import java.time.LocalDate;
import java.util.UUID;

public record OwnerReviewFilter(
        UUID ownerId,
        UUID venueId,
        UUID venueCourtId,
        Integer rating,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
) {
}
