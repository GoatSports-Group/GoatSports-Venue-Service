package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.infrastructure.external.client.NotificationClient;
import iuh.fit.venueservice.infrastructure.external.client.dto.OwnerApplicationProgressChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerApplicationNotificationAdapterTest {
    @Mock
    private NotificationClient notificationClient;
    @InjectMocks
    private OwnerApplicationNotificationAdapter adapter;

    @Test
    void notifyProgressChangedPublishesEventForApplicationOwner() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .userId(userId)
                .build();
        when(notificationClient.publishOwnerApplicationProgress(
                org.mockito.ArgumentMatchers.any(OwnerApplicationProgressChangedEvent.class)
        )).thenReturn(new BaseResponse<>(200, null, null, ""));

        adapter.notifyProgressChanged(application);

        ArgumentCaptor<OwnerApplicationProgressChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(OwnerApplicationProgressChangedEvent.class);
        verify(notificationClient).publishOwnerApplicationProgress(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo("OWNER_APPLICATION_PROGRESS_CHANGED");
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().ownerApplicationId()).isEqualTo(applicationId);
    }

    @Test
    void notifyProgressChangedRejectsFailedNotificationResponse() {
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();
        when(notificationClient.publishOwnerApplicationProgress(
                org.mockito.ArgumentMatchers.any(OwnerApplicationProgressChangedEvent.class)
        )).thenReturn(new BaseResponse<>(503, null, "Unavailable", "NOTIFICATION_UNAVAILABLE"));

        assertThrows(ServiceException.class, () -> adapter.notifyProgressChanged(application));
    }
}
