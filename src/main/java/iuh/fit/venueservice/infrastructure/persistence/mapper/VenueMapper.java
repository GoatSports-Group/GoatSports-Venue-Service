package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {VenueCourtMapper.class, VenueOwnerProfileMapper.class})
public interface VenueMapper {

    @Mapping(target = "venueOwnerProfile", ignore = true)
    @Mapping(target = "venueCourts", ignore = true)
    VenueEntity createEntity(Venue domain);

    void updateEntity(Venue domain, @MappingTarget VenueEntity entity);

    @Mapping(target = "venueOwnerProfile", ignore = true)
    @Mapping(target = "courts", ignore = true)
    Venue toDomain(VenueEntity entity);
}
