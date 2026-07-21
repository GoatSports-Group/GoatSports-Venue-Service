package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueCourtEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VenueCourtMapper {

    @Mapping(target = "venue", ignore = true)
    VenueCourtEntity createEntity(VenueCourt domain);

    void updateEntity(VenueCourt domain, @MappingTarget VenueCourtEntity entity);

    VenueCourt toDomain(VenueCourtEntity entity);

    default VenueCourtEntity createEntity(VenueCourt domain, VenueEntity parent) {
        VenueCourtEntity entity = createEntity(domain);
        if (entity != null) {
            entity.setVenue(parent);
        }
        return entity;
    }
}
