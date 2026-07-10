package iuh.fit.venueservice.application.dto.request.application;

import java.util.UUID;

public record SubmitOwnerApplicationCommand(
        UUID userId,
        String fullName,
        String phone,
        String email,
        String identityNumber,
        String businessName,
        String businessType,
        String taxCode,
        
        String address,
        String province,
        String district,
        String ward,
        String city
) {}
