package iuh.fit.venueservice.application.dto.response.venue;

import java.math.BigDecimal;

public record OwnerRevenueStatusBreakdown(
        String status,
        long paymentCount,
        BigDecimal nominalAmount
) {
}
