package iuh.fit.venueservice.infrastructure.activity;

import iuh.fit.venueservice.application.port.ActivityEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Tắt gợi ý cá nhân hóa ({@code activity.events.enabled=false}, vd. khi test): không phát gì. */
@Component
@ConditionalOnProperty(name = "activity.events.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpActivityEventPublisher implements ActivityEventPublisher {
    @Override
    public void publish(String eventType, UUID userId, Map<String, Object> payload) {
    }
}
