package iuh.fit.venueservice.application.dto.response.venue;

import java.math.BigDecimal;

public record OwnerHourlyRevenue(
        int hour,
        BigDecimal revenue,
        long succeededPaymentCount
) {
}
