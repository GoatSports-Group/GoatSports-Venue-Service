package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationDocumentRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
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

    public OwnerApplicationDocumentServiceImpl(
            StoragePort storagePort,
            OwnerApplicationRepository ownerApplicationRepository,
            OwnerApplicationDocumentRepository ownerApplicationDocumentRepository
    )
    {
        this.storagePort = storagePort;
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.ownerApplicationDocumentRepository = ownerApplicationDocumentRepository;
    }

    @Override
    public void createOwnerApplicationDocument(
            String ownerApplicationId, List<FileMoveRequest> requests
    ) throws ServiceException
    {
        UUID ownerApplicationIdUUID = BaseUtil.uuid(ownerApplicationId);

        List<String> storageResponse = storagePort.confirmFile(requests);

        for (String objectKey : storageResponse) {
            OwnerApplicationDocument document = OwnerApplicationDocument.builder()
                    .documentType(getDocumentType(objectKey))
                    .fileUrl(objectKey)
                    .build();

            ownerApplicationDocumentRepository.save(document, ownerApplicationIdUUID);
        }
    }

    private DocumentType getDocumentType(String path) throws ServiceException {
        if (path.contains("identities/")) {
            return DocumentType.IDENTITY_CARD;
        } else if (path.contains("licenses/")) {
            return DocumentType.BUSINESS_LICENSE;
        } else if (path.contains("venues/")) {
            return DocumentType.VENUE_PHOTO;
        } else {
            throw new ServiceException("Không nhận diện được loại tài liệu từ object key: " + path);
        }
    }
}
