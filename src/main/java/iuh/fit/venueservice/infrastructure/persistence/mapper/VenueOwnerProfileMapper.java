package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueOwnerProfileEntity;

public class VenueOwnerProfileMapper {

    public static VenueOwnerProfileEntity toEntity(VenueOwnerProfile domain) {
        if (domain == null) return null;

        OwnerApplicationEntity ownerAppEntity = domain.getOwnerApplication() != null
                ? OwnerApplicationMapper.toEntity(domain.getOwnerApplication())
                : null;

        VenueOwnerProfileEntity profileEntity = VenueOwnerProfileEntity.builder()
                .venueOwnerProfileId(domain.getVenueOwnerProfileId())
                .userId(domain.getUserId())
                .businessName(domain.getBusinessName())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .taxCode(domain.getTaxCode())
                .verified(domain.getVerified())
                .verifiedAt(domain.getVerifiedAt())
                .ownerApplication(ownerAppEntity)
                .build();

        if (ownerAppEntity != null) {
            ownerAppEntity.setVenueOwnerProfileEntity(profileEntity);
        }

        return profileEntity;
    }

    public static VenueOwnerProfile toDomain(VenueOwnerProfileEntity entity) {
        if (entity == null) return null;

        return VenueOwnerProfile.builder()
                .venueOwnerProfileId(entity.getVenueOwnerProfileId())
                .userId(entity.getUserId())
                .businessName(entity.getBusinessName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .taxCode(entity.getTaxCode())
                .verified(entity.getVerified())
                .verifiedAt(entity.getVerifiedAt())
                .ownerApplication(entity.getOwnerApplication() != null ? OwnerApplicationMapper.toDomain(entity.getOwnerApplication()) : null)
                .build();
    }
}
