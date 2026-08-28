package iuh.fit.venueservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicy extends BaseEntity {
    private UUID policyId;
    private Integer fullRefundHoursBefore;
    private Integer partialRefundHoursBefore;
    private Double partialRefundPercentage;
    private Integer noRefundHoursBefore;
}
