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

    /** Báo payment-service khoản thu này thuộc doanh thu của chủ sân nào (để chủ sân rút về ngân hàng). Idempotent. */
    void assignPayee(UUID paymentId, UUID payeeId);
}
