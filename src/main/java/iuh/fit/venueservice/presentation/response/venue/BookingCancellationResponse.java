package iuh.fit.venueservice.presentation.response.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancellationResponse {
    private UUID cancellationId;
    private UUID bookingId;
    private UUID refundId;
    private String reason;
    private Double refundPercentage;
    private Double refundAmount;
    private String status;
    private UUID processedBy;
    private String decisionReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
