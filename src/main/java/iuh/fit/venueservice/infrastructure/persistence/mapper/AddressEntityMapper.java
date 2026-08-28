package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.infrastructure.persistence.entity.AddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressEntityMapper {
    AddressEntity createEntity(Address domain);

    void updateEntity(Address domain, @MappingTarget AddressEntity entity);

    Address toDomain(AddressEntity entity);
}
