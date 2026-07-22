package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.venue.VenueResponseCommand;
import iuh.fit.venueservice.domain.entity.Venue;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueMapper {
    VenueResponseCommand toVenueResponseCommand(Venue venue);
}
