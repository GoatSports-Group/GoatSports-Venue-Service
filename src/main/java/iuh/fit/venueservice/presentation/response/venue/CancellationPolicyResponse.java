package iuh.fit.venueservice.presentation.response.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyResponse {
    private Integer fullRefundHoursBefore;
    private Integer partialRefundHoursBefore;
    private Double partialRefundPercentage;
    private Integer noRefundHoursBefore;
}
