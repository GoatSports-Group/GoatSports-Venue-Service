package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.dto.request.file.MoveFileRequest;
import iuh.fit.venueservice.application.port.OwnerApplicationBackgroundPort;
import iuh.fit.venueservice.application.port.OwnerApplicationNotificationPort;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerApplicationBackgroundAdapter implements OwnerApplicationBackgroundPort {
    private static final String CORRELATION_PREFIX = "owner-application:";

    private final OwnerApplicationRepository ownerApplicationRepository;
    private final StoragePort storagePort;
    private final OwnerApplicationNotificationPort notificationPort;

    @Override
    @Async
    public void schedule(UUID ownerApplicationId) {
        moveFiles(ownerApplicationId);
        sendNotification(ownerApplicationId);
    }

    @Scheduled(fixedDelayString = "${owner-application.background-retry-ms}")
    public void retryPendingFileMoves() {
        ownerApplicationRepository.findWithTemporaryDocuments(50)
                .forEach(application -> moveFiles(application.getOwnerApplicationId()));
    }

    private void moveFiles(UUID ownerApplicationId) {
        try {
            OwnerApplication application = ownerApplicationRepository.findById(ownerApplicationId).orElse(null);
            if (application == null) {
                return;
            }
            List<FileMoveRequest> requests = application.getDocuments().stream()
                    .map(OwnerApplicationDocument::getFileUrl)
                    .filter(fileUrl -> fileUrl != null && fileUrl.startsWith("temp/"))
                    .map(FileMoveRequest::new)
                    .toList();
            if (requests.isEmpty()) {
                return;
            }
            storagePort.moveFile(new MoveFileRequest(
                    requests,
                    CORRELATION_PREFIX + ownerApplicationId
            ));
        } catch (Exception exception) {
            log.error("Không thể gửi yêu cầu di chuyển file cho hồ sơ {}.", ownerApplicationId, exception);
        }
    }

    private void sendNotification(UUID ownerApplicationId) {
        try {
            OwnerApplication application = ownerApplicationRepository.findById(ownerApplicationId).orElse(null);
            if (application != null) {
                notificationPort.notifySubmitted(application);
            }
        } catch (Exception exception) {
            log.error("Không thể gửi thông báo hồ sơ chủ sân {}.", ownerApplicationId, exception);
        }
    }
}
