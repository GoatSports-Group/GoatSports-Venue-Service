package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.event.OwnerApplicationEventPublisher;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationDocumentRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerApplicationDocumentService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.domain.enums.DocumentType;

import java.util.List;
import java.util.UUID;

public class OwnerApplicationDocumentServiceImpl implements OwnerApplicationDocumentService {
    private final OwnerApplicationRepository ownerApplicationRepository;
    private final OwnerApplicationDocumentRepository ownerApplicationDocumentRepository;
    private final StoragePort storagePort;
    private final CurrentUserProvider currentUserProvider;
    private final OwnerApplicationEventPublisher ownerApplicationEventPublisher;

    public OwnerApplicationDocumentServiceImpl(
            StoragePort storagePort,
            OwnerApplicationRepository ownerApplicationRepository,
            OwnerApplicationDocumentRepository ownerApplicationDocumentRepository,
            CurrentUserProvider currentUserProvider,
            OwnerApplicationEventPublisher ownerApplicationEventPublisher
    )
    {
        this.storagePort = storagePort;
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.ownerApplicationDocumentRepository = ownerApplicationDocumentRepository;
        this.currentUserProvider = currentUserProvider;
        this.ownerApplicationEventPublisher = ownerApplicationEventPublisher;
    }

    @Override
    public void createOwnerApplicationDocument(
            String ownerApplicationId, List<FileMoveRequest> requests
    ) throws ServiceException
    {
        String currentUserId = currentUserProvider.getCurrentUserId().orElseThrow(
                () -> new ServiceException("Cần đăng nhập để thực hiện chức năng này")
        );
        UUID ownerApplicationIdUUID = BaseUtil.uuid(ownerApplicationId);
        OwnerApplication ownerApplication = ownerApplicationRepository.findById(ownerApplicationIdUUID)
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đăng ký: " + ownerApplicationId));

        if(!ownerApplication.getUserId().toString().equalsIgnoreCase(currentUserId)) {
            throw new ServiceException("Bạn không có quyền thực hiện hành động này");
        }

        List<String> storageResponse = storagePort.confirmFile(requests);

        for (String objectKey : storageResponse) {
            OwnerApplicationDocument document = OwnerApplicationDocument.builder()
                    .documentType(getDocumentType(objectKey))
                    .fileUrl(objectKey)
                    .build();

            ownerApplicationDocumentRepository.save(document, ownerApplication);
        }
    }

    private DocumentType getDocumentType(String path) {
        if (path.contains("identities/")) {
            return DocumentType.ID_CARD;
        } else if (path.contains("licenses/")) {
            return DocumentType.BUSINESS_LICENSE;
        } else if (path.contains("venues/")) {
            return DocumentType.VENUE_IMAGE;
        } else {
            return DocumentType.OTHER;
        }
    }
}
