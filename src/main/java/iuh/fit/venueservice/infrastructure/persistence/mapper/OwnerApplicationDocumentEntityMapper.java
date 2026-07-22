package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationDocumentEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OwnerApplicationDocumentEntityMapper {
    @Mapping(target = "ownerApplication", ignore = true)
    OwnerApplicationDocumentEntity createEntity(OwnerApplicationDocument domain);

    void updateEntity(OwnerApplicationDocument domain, @MappingTarget OwnerApplicationDocumentEntity entity);

    OwnerApplicationDocument toDomain(OwnerApplicationDocumentEntity entity);

    default OwnerApplicationDocumentEntity createEntity(OwnerApplicationDocument domain, OwnerApplicationEntity parent) {
        OwnerApplicationDocumentEntity entity = createEntity(domain);

        if (entity != null) {
            entity.setOwnerApplication(parent);
        }

        return entity;
    }
}
