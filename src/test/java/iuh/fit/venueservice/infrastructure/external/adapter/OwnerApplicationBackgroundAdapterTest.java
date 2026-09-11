package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.port.OwnerApplicationNotificationPort;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerApplicationBackgroundAdapterTest {
    @Mock
    private OwnerApplicationRepository ownerApplicationRepository;
    @Mock
    private StoragePort storagePort;
    @Mock
    private OwnerApplicationNotificationPort notificationPort;
    @InjectMocks
    private OwnerApplicationBackgroundAdapter adapter;

    @Test
    void schedulePublishesSubmissionAndProgressOutsideRequestThread() throws Exception {
        UUID applicationId = UUID.randomUUID();
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .userId(UUID.randomUUID())
                .status(OwnerApplicationStatus.PENDING)
                .documents(Set.of())
                .build();
        when(ownerApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        adapter.schedule(applicationId);

        verify(notificationPort).notifySubmitted(application);
        verify(notificationPort).notifyProgressChanged(application);
    }
}
