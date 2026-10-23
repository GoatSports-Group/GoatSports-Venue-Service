package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.port.ExternalVenueNotificationPort;
import iuh.fit.venueservice.infrastructure.external.client.NotificationClient;
import iuh.fit.venueservice.infrastructure.external.client.dto.CreateNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalVenueNotificationAdapter implements ExternalVenueNotificationPort {
    private final NotificationClient notificationClient;

    @Override
    public void notifySuggester(UUID receiverId, UUID suggestionId, String title, String content) {
        // Mỗi lượt gợi ý chỉ được duyệt một lần: event id cố định theo lượt gợi ý nên gửi lại không sinh thông báo trùng.
        UUID eventId = UUID.nameUUIDFromBytes(("external-venue-suggestion:" + suggestionId)
                .getBytes(StandardCharsets.UTF_8));
        try {
            notificationClient.createNotification(new CreateNotificationRequest(
                    eventId, receiverId, title, content, "SYSTEM", "VENUE", "EXTERNAL_VENUE_SUGGESTION", suggestionId
            ));
        } catch (RuntimeException exception) {
            log.warn("Không gửi được thông báo duyệt gợi ý sân {}", suggestionId, exception);
        }
    }
}
