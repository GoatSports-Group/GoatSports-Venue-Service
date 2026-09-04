package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class GenerateTimeSlotsRequest {
    @NotNull
    private LocalDate fromDate;
    @NotNull
    private LocalDate toDate;
    @NotEmpty
    private List<@NotNull UUID> pricingRuleIds;
    @NotNull
    @Min(30)
    @Max(240)
    private Integer slotDurationMinutes;
}
