package iuh.fit.venueservice.application.dto.response.venue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OwnerRevenueView(
        UUID scopeVenueId,
        String currency,
        String periodBasis,
        OwnerRevenuePeriod currentPeriod,
        OwnerRevenuePeriod previousPeriod,
        BigDecimal revenueChangePercentage,
        BigDecimal bookingCountChangePercentage,
        List<OwnerRevenueStatusBreakdown> paymentStatusBreakdown,
        List<OwnerDailyRevenue> dailyRevenue,
        List<OwnerHourlyRevenue> hourlyRevenue
) {
}
