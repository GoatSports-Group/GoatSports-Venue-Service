package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.request.payment.CreateRefundRequest;
import iuh.fit.venueservice.application.dto.request.payment.CompleteManualRefundRequest;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.infrastructure.external.client.PaymentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRefundAdapter implements PaymentRefundPort {
    private final PaymentClient paymentClient;

    @Override
    public RefundSnapshot createRefund(
            UUID paymentId,
            UUID requestedBy,
            String idempotencyKey,
            String reason,
            Double amount,
            Boolean autoDisburse
    ) {
        BaseResponse<RefundSnapshot> response = paymentClient.createRefund(
                paymentId,
                requestedBy,
                idempotencyKey,
                new CreateRefundRequest(reason, amount, autoDisburse)
        );
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Payment Service không trả về kết quả hoàn tiền.");
        }
        return response.getData();
    }

    @Override
    public void assignPayee(UUID paymentId, UUID payeeId) {
        paymentClient.assignPayee(paymentId, Map.of("payeeId", payeeId));
    }

    @Override
    public RefundSnapshot completeManualRefund(UUID refundId, UUID completedBy, String providerRefundId) {
        BaseResponse<RefundSnapshot> response = paymentClient.completeManualRefund(
                refundId, completedBy, new CompleteManualRefundRequest(providerRefundId)
        );
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Payment Service không trả về kết quả xác nhận hoàn tiền.");
        }
        return response.getData();
    }
}
