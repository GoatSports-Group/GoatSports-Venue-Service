package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.port.OwnerApplicationBackgroundPort;
import iuh.fit.venueservice.application.port.OwnerApplicationNotificationPort;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerApplicationDecoratorTest {
    @Mock
    private OwnerApplicationServiceImpl ownerApplicationServiceImpl;
    @Mock
    private OwnerApplicationNotificationPort notificationPort;
    @Mock
    private OwnerApplicationBackgroundPort backgroundPort;
    @InjectMocks
    private OwnerApplicationDecorator decorator;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void markViewedPublishesProgressOnlyAfterCommit() throws Exception {
        UUID applicationId = UUID.randomUUID();
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .userId(UUID.randomUUID())
                .build();
        when(ownerApplicationServiceImpl.markViewed(applicationId)).thenReturn(application);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        decorator.markViewed(applicationId);

        verify(notificationPort, never()).notifyProgressChanged(application);
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(notificationPort).notifyProgressChanged(application);
    }

    @Test
    void markViewedDoesNotPublishProgressWhenTransactionRollsBack() throws Exception {
        UUID applicationId = UUID.randomUUID();
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .userId(UUID.randomUUID())
                .build();
        when(ownerApplicationServiceImpl.markViewed(applicationId)).thenReturn(application);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        decorator.markViewed(applicationId);

        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        );
        verify(notificationPort, never()).notifyProgressChanged(application);
    }

    @Test
    void submitSchedulesAllPostCommitWorkInBackground() throws Exception {
        SubmitOwnerApplicationCommand command = mock(SubmitOwnerApplicationCommand.class);
        UUID applicationId = UUID.randomUUID();
        OwnerApplication application = OwnerApplication.builder()
                .ownerApplicationId(applicationId)
                .userId(UUID.randomUUID())
                .build();
        when(ownerApplicationServiceImpl.submitOwnerApplication(command)).thenReturn(application);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        decorator.submitOwnerApplication(command);

        verify(backgroundPort, never()).schedule(applicationId);
        verify(notificationPort, never()).notifyProgressChanged(application);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(backgroundPort).schedule(applicationId);
        verify(notificationPort, never()).notifyProgressChanged(application);
    }
}
