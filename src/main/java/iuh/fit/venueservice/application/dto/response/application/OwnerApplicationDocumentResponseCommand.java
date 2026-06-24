package iuh.fit.venueservice.application.dto.response.application;

import iuh.fit.venueservice.domain.enums.DocumentType;

import java.util.UUID;

public record OwnerApplicationDocumentResponseCommand(
        UUID ownerApplicationDocumentId,
        DocumentType documentType,
        String fileUrl
) {}
