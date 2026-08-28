package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.infrastructure.persistence.entity.BookingCancellationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookingCancellationEntityMapper {

    @Mapping(target = "booking", ignore = true)
    BookingCancellationEntity toEntity(BookingCancellation domain);

    @Mapping(target = "cancellationId", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(BookingCancellation domain, @MappingTarget BookingCancellationEntity entity);

    @Mapping(target = "bookingId", source = "booking.bookingId")
    BookingCancellation toDomain(BookingCancellationEntity entity);
}
