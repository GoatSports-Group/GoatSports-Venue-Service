package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.venue.VenueCourtResponseCommand;
import iuh.fit.venueservice.presentation.response.venue.VenueCourtResponse;

public class VenueCourtConverter {

    public static VenueCourtResponse toResponse(VenueCourtResponseCommand court) {
        if(court == null) return null;
        return VenueCourtResponse.builder()
                .venueCourtId(court.venueCourtId())
                .sportType(court.sportType().name())
                .name(court.name())
                .capacity(court.capacity())
                .pricePerHour(court.pricePerHour())
                .indoor(court.indoor())
                .active(court.active())
                .build();
    }

}
