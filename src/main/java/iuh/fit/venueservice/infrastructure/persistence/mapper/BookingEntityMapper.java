package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.infrastructure.persistence.entity.BookingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookingEntityMapper {

    @Mapping(target = "venueCourt", ignore = true)
    @Mapping(target = "cancellation", ignore = true)
    @Mapping(target = "checkInRecord", ignore = true)
    @Mapping(target = "review", ignore = true)
    BookingEntity toEntity(Booking domain);

    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "venueCourt", ignore = true)
    @Mapping(target = "cancellation", ignore = true)
    @Mapping(target = "checkInRecord", ignore = true)
    @Mapping(target = "review", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(Booking domain, @MappingTarget BookingEntity entity);

    @Mapping(target = "venueCourtId", source = "venueCourt.venueCourtId")
    @Mapping(target = "cancellation", ignore = true)
    @Mapping(target = "checkInRecord", ignore = true)
    @Mapping(target = "review", ignore = true)
    Booking toDomain(BookingEntity entity);
}
