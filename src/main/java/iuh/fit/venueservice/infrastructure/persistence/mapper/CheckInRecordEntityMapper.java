package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.CheckInRecord;
import iuh.fit.venueservice.infrastructure.persistence.entity.CheckInRecordEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CheckInRecordEntityMapper {
    @Mapping(target = "booking", ignore = true)
    CheckInRecordEntity toEntity(CheckInRecord domain);

    @Mapping(target = "bookingId", source = "booking.bookingId")
    CheckInRecord toDomain(CheckInRecordEntity entity);
}
