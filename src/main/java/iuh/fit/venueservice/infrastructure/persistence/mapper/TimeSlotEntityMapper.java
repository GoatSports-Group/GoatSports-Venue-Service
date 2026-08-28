package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.infrastructure.persistence.entity.TimeSlotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TimeSlotEntityMapper {

    @Mapping(target = "venueCourt", ignore = true)
    TimeSlotEntity toEntity(TimeSlot domain);

    @Mapping(target = "venueCourtId", source = "venueCourt.venueCourtId")
    TimeSlot toDomain(TimeSlotEntity entity);
}
