package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.infrastructure.persistence.entity.AddressEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressEntityMapper {
    @Mapping(target = "ownerApplication", ignore = true)
    AddressEntity createEntity(Address domain);

    void updateEntity(Address domain, @MappingTarget AddressEntity entity);

    Address toDomain(AddressEntity entity);

    default AddressEntity createEntity(Address domain, OwnerApplicationEntity parent){
        AddressEntity entity = createEntity(domain);
        entity.setOwnerApplication(parent);
        return entity;
    }
}