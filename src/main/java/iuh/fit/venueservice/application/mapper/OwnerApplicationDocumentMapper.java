package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationDocumentResponseCommand;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;

public class OwnerApplicationDocumentMapper {

    public static OwnerApplicationDocumentResponseCommand toOwnerApplicationDocumentResponseCommand(OwnerApplicationDocument document) {
        return new OwnerApplicationDocumentResponseCommand(
                document.getOwnerApplicationDocumentId(),
                document.getDocumentType(),
                document.getFileUrl()
        );
    }

}
