package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.DocumentType;
import java.util.List;
import java.util.UUID;

public record SubmitOwnerApplicationCommand(
        UUID userId,
        String fullName,
        String phone,
        String email,
        String identityNumber,
        String businessName,
        BusinessType businessType,
        String taxCode,
        
        String address,
        String province,
        String district,
        String ward,
        String city,

        List<DocumentInfo> documents
) {
    public record DocumentInfo(
            DocumentType documentType,
            byte[] content,
            String originalFileName,
            String contentType
    ) {}
}
