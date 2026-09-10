package iuh.fit.venueservice.infrastructure.external.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerApplicationNotificationEvent {
    private UUID eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private OwnerApplicationData data;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerApplicationData {
        private UUID ownerApplicationId;
        private UUID userId;
        private String email;
        private String status;
        private String rejectReason;
    }
}
