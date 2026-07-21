package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;

import java.util.UUID;

public interface OwnerApplicationDocumentRepository {
    OwnerApplicationDocument save(OwnerApplicationDocument document, UUID ownerApplicationId) throws ServiceException;
}
