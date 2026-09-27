package iuh.fit.venueservice.infrastructure.external.client.dto;

import java.util.UUID;

public record CreateNotificationRequest(
        UUID eventId,
        UUID receiverId,
        String title,
        String content,
        String type,
        String sourceService,
        String referenceType,
        UUID referenceId
) {}
