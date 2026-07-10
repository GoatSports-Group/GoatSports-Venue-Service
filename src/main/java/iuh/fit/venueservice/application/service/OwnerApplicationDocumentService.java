package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.List;

public interface OwnerApplicationDocumentService {
    void createOwnerApplicationDocument(String ownerApplicationId, List<FileMoveRequest> requests) throws ServiceException;
}
