package iuh.fit.venueservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment.events",
        name = "consumer-enabled",
        havingValue = "true"
)
public class PaymentEventConsumer {
    private final ObjectMapper objectMapper;
    private final BookingPaymentEventService bookingPaymentEventService;

    @KafkaListener(
            topics = "${payment.events.topic}",
            groupId = "${payment.events.venue-group-id}"
    )
    @Transactional
    public void consume(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            String eventType = envelope.path("eventType").asText();
            JsonNode payload = envelope.path("payload");
            if ("reminder.refund-pending.v1".equals(eventType)) {
                if ("MANUAL_REVIEW".equals(payload.path("status").asText())) {
                    bookingPaymentEventService.remindOwnerOfManualRefund(uuid(payload, "refundId"),
                            uuid(envelope, "eventId"), payload.path("amount").asDouble());
                }
                return;
            }
            // refund.*: workflow-service đưa vào process hủy đặt sân trên Camunda; trạng thái yêu cầu hủy chỉ đổi
            // khi process ghi lại (BookingCancellationService.applyWorkflowState), venue-service không tự đọc nữa.
            if ("payment.succeeded.v1".equals(eventType)) {
                bookingPaymentEventService.applySucceededPayment(new PaymentSucceededCommand(
                        uuid(payload, "paymentId"), uuid(payload, "payerId"),
                        payload.path("purpose").asText(), payload.path("referenceType").asText(),
                        uuid(payload, "referenceId"), payload.path("amount").asDouble(),
                        payload.path("currency").asText(), payload.path("status").asText()
                ));
                return;
            }
            if ("payment.failed.v1".equals(eventType)
                    || "payment.cancelled.v1".equals(eventType)
                    || "payment.expired.v1".equals(eventType)) {
                bookingPaymentEventService.applyTerminalPayment(new PaymentTerminalCommand(
                        uuid(payload, "paymentId"), uuid(payload, "payerId"),
                        payload.path("purpose").asText(), payload.path("referenceType").asText(),
                        uuid(payload, "referenceId"), payload.path("status").asText(),
                        nullableText(payload, "failureCode"), nullableText(payload, "failureReason")
                ));
            }
        } catch (ServiceException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Payment event không đúng contract.", exception);
        }
    }

    private UUID uuid(JsonNode node, String field) {
        return UUID.fromString(node.path(field).asText());
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank()
                ? null
                : value.asText();
    }
}
