package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.OwnerApplicationBackgroundPort;
import iuh.fit.venueservice.application.port.OwnerApplicationNotificationPort;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class OwnerApplicationDecorator implements OwnerApplicationService {
    private final OwnerApplicationServiceImpl ownerApplicationServiceImpl;
    private final OwnerApplicationNotificationPort notificationPort;
    private final OwnerApplicationBackgroundPort backgroundPort;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication submitOwnerApplication(SubmitOwnerApplicationCommand command) throws ServiceException {
        OwnerApplication application = ownerApplicationServiceImpl.submitOwnerApplication(command);
        afterCommit(() -> backgroundPort.schedule(application.getOwnerApplicationId()));
        return application;
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void markDocumentsMoved(UUID applicationId) throws ServiceException {
        ownerApplicationServiceImpl.markDocumentsMoved(applicationId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication rejectOwnerApplication(UUID applicationId, UUID reviewerId, String rejectReason) throws ServiceException {
        OwnerApplication application = ownerApplicationServiceImpl.rejectOwnerApplication(
                applicationId,
                reviewerId,
                rejectReason
        );
        afterCommit(() -> notificationPort.notifyRejected(application));
        return application;
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication approveOwnerApplication(UUID applicationId, UUID reviewerId) throws ServiceException {
        OwnerApplication application = ownerApplicationServiceImpl.approveOwnerApplication(applicationId, reviewerId);
        afterCommit(() -> notificationPort.notifyApproved(application));
        return application;
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerApplication markViewed(UUID applicationId) throws ServiceException {
        return ownerApplicationServiceImpl.markViewed(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerApplication getOwnerApplicationDetail(String id) throws ServiceException {
        return ownerApplicationServiceImpl.getOwnerApplicationDetail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerApplication> getMyOwnerApplications(PageFilter filter, UUID userId) {
        return ownerApplicationServiceImpl.getMyOwnerApplications(filter, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerApplication> getAllOwnerApplications(PageFilter filter) {
        return ownerApplicationServiceImpl.getAllOwnerApplications(filter);
    }

    private void afterCommit(PostCommitAction action) {
        Runnable callback = () -> {
            try {
                action.run();
            } catch (Exception exception) {
                log.error("Không thể thực thi công việc nền của hồ sơ chủ sân.", exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    callback.run();
                }
            });
            return;
        }
        callback.run();
    }

    @FunctionalInterface
    private interface PostCommitAction {
        void run() throws Exception;
    }
}
