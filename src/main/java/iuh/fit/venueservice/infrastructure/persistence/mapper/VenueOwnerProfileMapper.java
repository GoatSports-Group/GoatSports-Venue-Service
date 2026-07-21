package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueOwnerProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VenueOwnerProfileMapper {

    @Mapping(target = "ownerApplication", ignore = true)
    @Mapping(target = "venue", ignore = true)
    VenueOwnerProfileEntity createEntity(VenueOwnerProfile domain);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "ownerApplication", ignore = true)
    @Mapping(target = "venue", ignore = true)
    void updateEntity(VenueOwnerProfile domain, @MappingTarget VenueOwnerProfileEntity entity);

    VenueOwnerProfile toDomain(VenueOwnerProfileEntity entity);

    default VenueOwnerProfileEntity createEntity(VenueOwnerProfile domain, OwnerApplicationEntity parent) {
        VenueOwnerProfileEntity entity = createEntity(domain);

        if (entity != null) {
            entity.setOwnerApplication(parent);
            if (parent != null) {
                parent.setVenueOwnerProfile(entity);
            }
        }

        return entity;
    }
}
