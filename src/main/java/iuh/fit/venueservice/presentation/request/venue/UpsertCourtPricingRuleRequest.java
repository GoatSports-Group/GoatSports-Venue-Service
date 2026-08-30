package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class UpsertCourtPricingRuleRequest {
    @NotNull
    private DayOfWeek dayOfWeek;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    @NotNull
    @DecimalMin(value = "0.01")
    private Double pricePerHour;
    @NotNull
    @DecimalMin(value = "0.01")
    private Double basePricePerHour;
    @NotNull
    private LocalDate effectiveFrom;
    @NotNull
    private LocalDate effectiveTo;
}
