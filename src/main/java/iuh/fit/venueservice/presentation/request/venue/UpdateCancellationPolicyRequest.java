package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCancellationPolicyRequest(
        @NotNull @Min(0) @Max(8760) Integer fullRefundHoursBefore,
        @NotNull @Min(0) @Max(8760) Integer partialRefundHoursBefore,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") Double partialRefundPercentage,
        @NotNull @Min(0) @Max(8760) Integer noRefundHoursBefore
) {
}
