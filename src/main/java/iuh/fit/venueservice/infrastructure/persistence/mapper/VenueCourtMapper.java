package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueCourtEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;

public class VenueCourtMapper {

    public static VenueCourtEntity toEntity(VenueCourt domain, VenueEntity parent) {
        if (domain == null) return null;

        return VenueCourtEntity.builder()
                .venueCourtId(domain.getVenueCourtId())
                .sportType(domain.getSportType())
                .name(domain.getName())
                .capacity(domain.getCapacity())
                .pricePerHour(domain.getPricePerHour())
                .indoor(domain.getIndoor())
                .active(domain.getActive())
                .venue(parent)
                .build();
    }

    public static VenueCourt toDomain(VenueCourtEntity entity) {
        if (entity == null) return null;

        return VenueCourt.builder()
                .venueCourtId(entity.getVenueCourtId())
                .sportType(entity.getSportType())
                .name(entity.getName())
                .capacity(entity.getCapacity())
                .pricePerHour(entity.getPricePerHour())
                .indoor(entity.getIndoor())
                .active(entity.getActive())
                .build();
    }
}
