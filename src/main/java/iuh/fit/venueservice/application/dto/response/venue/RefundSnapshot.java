package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefundSnapshot(
        UUID refundId,
        UUID paymentId,
        UUID requestedBy,
        String reason,
        Double amount,
        String providerRefundId,
        String status,
        String failureReason,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
