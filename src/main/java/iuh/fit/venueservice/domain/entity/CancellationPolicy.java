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

    /**
     * Phần trăm tiền cọc được hoàn khi người chơi hủy trước giờ chơi {@code hoursBefore} giờ:
     * đủ sớm thì hoàn 100%, trong khoảng giữa thì hoàn theo tỷ lệ chủ sân đặt, sát giờ thì không hoàn.
     */
    public double refundPercentage(long hoursBefore) {
        if (hoursBefore >= fullRefundHoursBefore) return 100.0;
        if (hoursBefore >= partialRefundHoursBefore) {
            return partialRefundPercentage == null ? 0.0 : partialRefundPercentage;
        }
        return 0.0;
    }
}
