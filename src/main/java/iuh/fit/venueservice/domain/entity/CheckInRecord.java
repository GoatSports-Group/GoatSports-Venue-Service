package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.CheckInMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRecord extends BaseEntity {
    private UUID checkInId;
    private UUID bookingId;
    private UUID paymentId;
    private UUID checkedInBy;
    private CheckInMethod method;
    private BigDecimal remainingAmountCollected;
    private LocalDateTime checkedInAt;
}
