package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;

import java.util.Collections;
import java.util.stream.Collectors;

public class OwnerApplicationMapper {

    public static OwnerApplicationEntity toEntity(OwnerApplication domain) {
        if (domain == null) return null;

        OwnerApplicationEntity entity =
                OwnerApplicationEntity.builder()
                        .ownerApplicationId(domain.getOwnerApplicationId())
                        .userId(domain.getUserId())
                        .fullName(domain.getFullName())
                        .phone(domain.getPhone())
                        .email(domain.getEmail())
                        .businessName(domain.getBusinessName())
                        .businessType(domain.getBusinessType())
                        .taxCode(domain.getTaxCode())
                        .identityNumber(domain.getIdentityNumber())
                        .status(domain.getStatus())
                        .rejectReason(domain.getRejectReason())
                        .reviewerId(domain.getReviewerId())
                        .reviewedAt(domain.getReviewedAt())
                        .build();

        if (domain.getAddress() != null) {
            entity.setAddresses(AddressMapper.toEntity(domain.getAddress(), entity));
        }

        return entity;
    }

    public static OwnerApplication toDomain(OwnerApplicationEntity entity) {
        if (entity == null) return null;

        return OwnerApplication.builder()
                .ownerApplicationId(entity.getOwnerApplicationId())
                .userId(entity.getUserId())
                .fullName(entity.getFullName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .businessName(entity.getBusinessName())
                .businessType(entity.getBusinessType())
                .taxCode(entity.getTaxCode())
                .identityNumber(entity.getIdentityNumber())
                .status(entity.getStatus())
                .rejectReason(entity.getRejectReason())
                .reviewerId(entity.getReviewerId())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .address(AddressMapper.toDomain(entity.getAddresses()))
                .build();
    }
}
