package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.infrastructure.persistence.entity.TimeSlotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TimeSlotEntityMapper {

    @Mapping(target = "venueCourt", ignore = true)
    TimeSlotEntity toEntity(TimeSlot domain);

    @Mapping(target = "timeSlotId", ignore = true)
    @Mapping(target = "venueCourt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    void updateEntity(TimeSlot domain, @MappingTarget TimeSlotEntity entity);

    @Mapping(target = "venueCourtId", source = "venueCourt.venueCourtId")
    TimeSlot toDomain(TimeSlotEntity entity);
}
