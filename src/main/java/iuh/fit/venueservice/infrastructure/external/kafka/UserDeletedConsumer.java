package iuh.fit.venueservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.application.service.AccountDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** auth-service vừa ẩn danh hóa một tài khoản (hết 30 ngày chờ xóa): dọn phần dữ liệu định danh ở venue-service. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-deleted.consumer-enabled", havingValue = "true")
public class UserDeletedConsumer {
    private final ObjectMapper objectMapper;
    private final AccountDataService accountDataService;

    @KafkaListener(topics = "${user-deleted.topic}", groupId = "${user-deleted.venue-group-id}")
    @Transactional
    public void consume(String message) throws Exception {
        UUID userId = UUID.fromString(objectMapper.readTree(message).path("userId").asText());
        accountDataService.eraseUserData(userId);
        log.info("Erased venue data of deleted user {}", userId);
    }
}
