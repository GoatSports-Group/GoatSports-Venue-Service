package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.port.BookingNotificationPort;
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
public class BookingNotificationAdapter implements BookingNotificationPort {
    private final NotificationClient notificationClient;

    @Override
    public void notifyOwner(UUID ownerId, UUID bookingId, String dedupeKey, String title, String content) {
        notifyPayer(ownerId, bookingId, dedupeKey, title, content);
    }

    @Override
    public void notifyPayer(UUID receiverId, UUID bookingId, String dedupeKey, String title, String content) {
        UUID eventId = UUID.nameUUIDFromBytes(dedupeKey.getBytes(StandardCharsets.UTF_8));
        try {
            notificationClient.createNotification(new CreateNotificationRequest(
                    eventId, receiverId, title, content, "BOOKING", "VENUE", "BOOKING", bookingId
            ));
        } catch (RuntimeException exception) {
            log.warn("Không gửi được thông báo booking {} (event {})", bookingId, eventId, exception);
        }
    }
}
