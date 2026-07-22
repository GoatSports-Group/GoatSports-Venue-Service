package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.venue.VenueCourtResponseCommand;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueCourtMapper {
    VenueCourtResponseCommand toVenueCourtResponseCommand(VenueCourt venueCourt);
}
