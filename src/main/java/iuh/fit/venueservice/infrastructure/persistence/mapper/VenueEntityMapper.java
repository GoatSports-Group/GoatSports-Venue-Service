package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.persistence.entity.AddressEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AddressEntityMapper.class, VenueCourtEntityMapper.class})
public interface VenueEntityMapper {

    @Mapping(target = "venueCourts", ignore = true)
    @Mapping(target = "cancellationPolicy", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    VenueEntity createEntity(Venue domain);

    @Mapping(target = "address", ignore = true)
    @Mapping(target = "venueCourts", ignore = true)
    @Mapping(target = "cancellationPolicy", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    void updateEntity(Venue domain, @MappingTarget VenueEntity entity);

    @Mapping(target = "courts", source = "venueCourts")
    @Mapping(target = "reviews", ignore = true)
    Venue toDomain(VenueEntity entity);

    default void updateEntity(Venue domain, VenueEntity entity, AddressEntityMapper addressEntityMapper) {
        if (domain == null || entity == null) return;
        updateEntity(domain, entity);
        if (domain.getAddress() == null) return;
        AddressEntity address = entity.getAddress();
        if (address == null) {
            entity.setAddress(addressEntityMapper.createEntity(domain.getAddress()));
        } else {
            addressEntityMapper.updateEntity(domain.getAddress(), address);
        }
    }
}
