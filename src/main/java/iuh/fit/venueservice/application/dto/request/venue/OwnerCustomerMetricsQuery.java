package iuh.fit.venueservice.application.dto.request.venue;

import java.time.YearMonth;
import java.util.UUID;

public record OwnerCustomerMetricsQuery(
        UUID ownerId,
        UUID venueId,
        YearMonth month
) {
}
