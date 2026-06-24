package iuh.fit.venueservice.application.dto.response;

import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record OwnerApplicationResponseCommand(
        UUID ownerApplicationId,
        UUID userId,
        String fullName,
        String phone,
        String email,
        String businessName,
        BusinessType businessType,
        String taxCode,
        String identityNumber,
        OwnerApplicationStatus status,
        String rejectReason,
        UUID reviewerId,
        LocalDateTime reviewedAt,
        Address address,
        Set<OwnerApplicationDocumentResponseCommand> documents
) {}
