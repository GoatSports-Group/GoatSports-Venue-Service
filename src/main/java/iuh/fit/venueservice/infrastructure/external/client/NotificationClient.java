package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.infrastructure.external.client.dto.CreateNotificationRequest;
import iuh.fit.venueservice.infrastructure.external.client.dto.NotificationResponseCommand;
import iuh.fit.venueservice.infrastructure.external.client.dto.OwnerApplicationNotificationEvent;
import iuh.fit.venueservice.infrastructure.external.client.dto.OwnerApplicationProgressChangedEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "GOATSPORTS-NOTIFICATION-SERVICE",
        url = "${notification.service.url}",
        path = "/internal/notifications",
        configuration = StorageClientConfig.class
)
public interface NotificationClient {
    /** Tạo và đẩy realtime một thông báo; cùng eventId chỉ tạo một lần. */
    @PostMapping
    void createNotification(@RequestBody CreateNotificationRequest request);

    @PostMapping("/owner-applications")
    BaseResponse<NotificationResponseCommand> createOwnerApplicationNotification(
            @RequestBody OwnerApplicationNotificationEvent event
    );

    @PostMapping("/notify")
    BaseResponse<Void> notifyUser(@RequestBody NotificationResponseCommand notification);

    @PostMapping("/owner-application-progress")
    BaseResponse<Void> publishOwnerApplicationProgress(@RequestBody OwnerApplicationProgressChangedEvent event);
}
