package iuh.fit.venueservice.application.dto.response.application;

import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;

import java.time.LocalDateTime;
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
        LocalDateTime createdAt,
        UUID reviewerId,
        LocalDateTime reviewedAt,
        Address address
) {}
