package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;

import java.util.UUID;

public interface PaymentRefundPort {
    RefundSnapshot createRefund(
            UUID paymentId,
            UUID requestedBy,
            String idempotencyKey,
            String reason,
            Double amount,
            Boolean autoDisburse
    );

    RefundSnapshot completeManualRefund(UUID refundId, UUID completedBy, String providerRefundId);
}
