package iuh.fit.venueservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerApplicationFileMoveCompletedConsumer {
    private static final String CORRELATION_PREFIX = "owner-application:";

    private final ObjectMapper objectMapper;
    private final OwnerApplicationService ownerApplicationService;

    @KafkaListener(
            topics = "${file.events.move-completed-topic}",
            groupId = "${file.events.venue-group-id}"
    )
    public void consume(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String correlationId = event.path("correlationId").asText();
            if (!correlationId.startsWith(CORRELATION_PREFIX)) {
                return;
            }
            UUID ownerApplicationId = UUID.fromString(correlationId.substring(CORRELATION_PREFIX.length()));
            if (!event.path("success").asBoolean(false)) {
                log.error(
                        "Di chuyển file thất bại cho hồ sơ {}: {}",
                        ownerApplicationId,
                        event.path("failureReason").asText("Không xác định")
                );
                return;
            }
            ownerApplicationService.markDocumentsMoved(ownerApplicationId);
        } catch (Exception exception) {
            log.error("Không thể xử lý kết quả di chuyển file: {}", message, exception);
            throw new IllegalArgumentException("File move completion event không đúng contract.", exception);
        }
    }
}
