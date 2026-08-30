package iuh.fit.venueservice.application.dto.request.venue;

import java.time.LocalDate;
import java.util.UUID;

public record OwnerRevenueQuery(
        UUID ownerId,
        UUID venueId,
        LocalDate fromDate,
        LocalDate toDate
) {
}
