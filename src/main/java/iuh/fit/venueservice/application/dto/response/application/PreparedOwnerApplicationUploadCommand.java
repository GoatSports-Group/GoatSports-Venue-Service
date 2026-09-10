package iuh.fit.venueservice.application.dto.response.application;

import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;

import java.util.List;
import java.util.UUID;

public record PreparedOwnerApplicationUploadCommand(
        UUID ownerApplicationId,
        List<Document> documents
) {
    public record Document(
            OwnerApplicationDocumentSlot slot,
            String uploadUrl,
            String objectKey
    ) {}
}
