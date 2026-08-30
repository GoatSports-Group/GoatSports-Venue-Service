package iuh.fit.venueservice.application.dto.response.venue;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OwnerRevenuePeriod(
        LocalDate fromDate,
        LocalDate toDate,
        long bookingCount,
        long paidBookingCount,
        BigDecimal totalRevenue
) {
}
