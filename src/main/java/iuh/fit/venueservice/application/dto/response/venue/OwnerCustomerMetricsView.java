package iuh.fit.venueservice.application.dto.response.venue;

import java.util.UUID;

public record OwnerCustomerMetricsView(
        UUID scopeVenueId,
        String periodBasis,
        OwnerCustomerMetricsPeriod currentPeriod,
        OwnerCustomerMetricsPeriod previousPeriod
) {
}
