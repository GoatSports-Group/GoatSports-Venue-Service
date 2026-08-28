package iuh.fit.venueservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourtPricingRule extends BaseEntity {
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
