package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.venue.VenueCourtResponseCommand;
import iuh.fit.venueservice.presentation.response.venue.VenueCourtResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueCourtConverter {
    VenueCourtResponse toResponse(VenueCourtResponseCommand court);
}
