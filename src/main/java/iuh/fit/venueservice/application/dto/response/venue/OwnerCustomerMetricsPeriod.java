package iuh.fit.venueservice.application.dto.response.venue;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OwnerCustomerMetricsPeriod(
        LocalDate fromDate,
        LocalDate toDate,
        long totalCustomers,
        long newCustomers,
        long returningCustomers,
        BigDecimal returningRate
) {
}
