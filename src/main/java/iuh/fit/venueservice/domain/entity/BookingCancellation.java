package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.CancellationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookingCancellation extends BaseEntity {
    private UUID cancellationId;
    private UUID bookingId;
    private UUID refundId;
    private String reason;
    private Double refundPercentage;
    private Double refundAmount;
    private CancellationStatus status;
    private UUID processedBy;
    private String decisionReason;
    private LocalDateTime processedAt;
}
