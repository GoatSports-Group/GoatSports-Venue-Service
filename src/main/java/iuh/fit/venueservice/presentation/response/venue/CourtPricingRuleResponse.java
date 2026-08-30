package iuh.fit.venueservice.presentation.response.venue;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
public class CourtPricingRuleResponse {
    private UUID pricingRuleId;
    private UUID courtId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double pricePerHour;
    private Double basePricePerHour;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
