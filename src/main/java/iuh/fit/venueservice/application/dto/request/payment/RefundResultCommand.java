package iuh.fit.venueservice.application.dto.request.payment;

import java.util.UUID;

public record RefundResultCommand(
        UUID refundId,
        UUID paymentId,
        String status,
        String failureReason
) {
}
