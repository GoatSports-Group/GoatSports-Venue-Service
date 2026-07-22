package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.venue.VenueResponseCommand;
import iuh.fit.venueservice.presentation.response.venue.VenueResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueConverter {
    VenueResponse toResponse(VenueResponseCommand venue);
}
