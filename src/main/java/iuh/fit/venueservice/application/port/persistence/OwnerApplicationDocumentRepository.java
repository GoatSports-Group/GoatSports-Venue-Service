package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;

public interface OwnerApplicationDocumentRepository {
    OwnerApplicationDocument save(OwnerApplicationDocument document, OwnerApplication ownerApplication);
}
