package iuh.fit.venueservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
            if (!"payment.succeeded.v1".equals(envelope.path("eventType").asText())) return;
            JsonNode payload = envelope.path("payload");
            bookingPaymentEventService.applySucceededPayment(new PaymentSucceededCommand(
                    uuid(payload, "paymentId"), uuid(payload, "payerId"),
                    payload.path("purpose").asText(), payload.path("referenceType").asText(),
                    uuid(payload, "referenceId"), payload.path("amount").asDouble(),
                    payload.path("currency").asText(), payload.path("status").asText()
            ));
        } catch (ServiceException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Payment event không đúng contract.", exception);
        }
    }

    private java.util.UUID uuid(JsonNode node, String field) {
        return java.util.UUID.fromString(node.path(field).asText());
    }
}
