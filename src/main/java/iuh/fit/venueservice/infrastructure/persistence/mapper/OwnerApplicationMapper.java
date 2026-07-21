package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, OwnerApplicationDocumentMapper.class})
public interface OwnerApplicationMapper {

    @Mapping(target = "address", ignore = true)
    @Mapping(target = "ownerApplicationDocuments", ignore = true)
    @Mapping(target = "venueOwnerProfile", ignore = true)
    OwnerApplicationEntity createEntity(OwnerApplication domain);

    @Mapping(target = "ownerApplicationId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "ownerApplicationDocuments", ignore = true)
    @Mapping(target = "venueOwnerProfile", ignore = true)
    void updateEntity(OwnerApplication domain, @MappingTarget OwnerApplicationEntity entity);

    @Mapping(target = "documents", ignore = true)
    OwnerApplication toDomain(OwnerApplicationEntity entity);

    default OwnerApplicationEntity createEntity(OwnerApplication domain, AddressMapper addressMapper) {
        OwnerApplicationEntity entity = createEntity(domain);

        if (entity != null && domain != null && domain.getAddress() != null && addressMapper != null) {
            entity.setAddress(addressMapper.createEntity(domain.getAddress(), entity));
        }

        return entity;
    }

    default void updateEntity(OwnerApplication domain, OwnerApplicationEntity entity, AddressMapper addressMapper) {
        if (domain == null || entity == null) return;

        updateEntity(domain, entity);

        if (domain.getAddress() != null && addressMapper != null) {
            if (entity.getAddress() == null) {
                entity.setAddress(addressMapper.createEntity(domain.getAddress(), entity));
            } else {
                addressMapper.updateEntity(domain.getAddress(), entity.getAddress());
            }
        }
    }
}
