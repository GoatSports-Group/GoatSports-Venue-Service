package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationDocumentEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;

public class OwnerApplicationDocumentMapper {

    public static OwnerApplicationDocumentEntity toEntity(OwnerApplicationDocument domain, OwnerApplicationEntity parent) {
        if (domain == null) return null;

        return OwnerApplicationDocumentEntity.builder()
                .ownerApplicationDocumentId(domain.getOwnerApplicationDocumentId())
                .documentType(domain.getDocumentType())
                .fileUrl(domain.getFileUrl())
                .ownerApplication(parent)
                .build();
    }

    public static OwnerApplicationDocument toDomain(OwnerApplicationDocumentEntity entity) {
        if (entity == null) return null;

        return OwnerApplicationDocument.builder()
                .ownerApplicationDocumentId(entity.getOwnerApplicationDocumentId())
                .documentType(entity.getDocumentType())
                .fileUrl(entity.getFileUrl())
                .build();
    }
}
