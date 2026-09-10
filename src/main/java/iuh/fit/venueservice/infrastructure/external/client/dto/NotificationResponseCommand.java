package iuh.fit.venueservice.infrastructure.external.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponseCommand(
        UUID notificationId,
        UUID receiverId,
        String title,
        String content,
        String type,
        String status,
        UUID referenceId,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}
