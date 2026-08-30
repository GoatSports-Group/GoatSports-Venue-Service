package iuh.fit.venueservice.application.dto.response.venue;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OwnerDailyRevenue(
        LocalDate date,
        BigDecimal revenue,
        long succeededPaymentCount
) {
}
