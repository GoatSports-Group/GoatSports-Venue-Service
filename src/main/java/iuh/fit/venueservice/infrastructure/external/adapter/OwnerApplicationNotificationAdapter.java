package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.OwnerApplicationNotificationPort;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.infrastructure.external.client.NotificationClient;
import iuh.fit.venueservice.infrastructure.external.client.dto.NotificationResponseCommand;
import iuh.fit.venueservice.infrastructure.external.client.dto.OwnerApplicationNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerApplicationNotificationAdapter implements OwnerApplicationNotificationPort {
    private final NotificationClient notificationClient;

    @Override
    public void notifySubmitted(OwnerApplication application) throws ServiceException {
        publish(
                application.getOwnerApplicationId(),
                "SUBMITTED",
                application.getOwnerApplicationId(),
                application.getUserId(),
                application.getEmail(),
                application.getStatus().name(),
                null
        );
    }

    @Override
    public void notifyApproved(OwnerApplication application) throws ServiceException {
        publish(
                UUID.randomUUID(),
                "APPROVED",
                application.getOwnerApplicationId(),
                application.getUserId(),
                application.getEmail(),
                application.getStatus().name(),
                null
        );
    }

    @Override
    public void notifyRejected(OwnerApplication application) throws ServiceException {
        publish(
                UUID.randomUUID(),
                "REJECTED",
                application.getOwnerApplicationId(),
                application.getUserId(),
                application.getEmail(),
                application.getStatus().name(),
                application.getRejectReason()
        );
    }

    private void publish(
            UUID eventId,
            String eventType,
            UUID ownerApplicationId,
            UUID userId,
            String email,
            String status,
            String rejectReason
    ) throws ServiceException {
        OwnerApplicationNotificationEvent event = OwnerApplicationNotificationEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .data(OwnerApplicationNotificationEvent.OwnerApplicationData.builder()
                        .ownerApplicationId(ownerApplicationId)
                        .userId(userId)
                        .email(email)
                        .status(status)
                        .rejectReason(rejectReason)
                        .build())
                .build();
        var createResponse = notificationClient.createOwnerApplicationNotification(event);
        NotificationResponseCommand notification = createResponse == null ? null : createResponse.getData();
        if (notification == null) {
            throw new ServiceException("Notification Service không tạo được thông báo hồ sơ chủ sân.");
        }
        try {
            notificationClient.notifyUser(notification);
        } catch (RuntimeException exception) {
            log.warn("Notification đã được lưu nhưng không thể push realtime: {}", notification.notificationId(), exception);
        }
    }
}
