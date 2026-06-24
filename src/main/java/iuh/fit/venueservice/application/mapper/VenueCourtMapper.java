package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.venue.VenueCourtResponseCommand;
import iuh.fit.venueservice.domain.entity.VenueCourt;

public class VenueCourtMapper {

    public static VenueCourtResponseCommand toVenueCourtResponseCommand(VenueCourt venueCourt) {
        return new VenueCourtResponseCommand(
                venueCourt.getVenueCourtId(),
                venueCourt.getSportType(),
                venueCourt.getName(),
                venueCourt.getCapacity(),
                venueCourt.getPricePerHour(),
                venueCourt.getIndoor(),
                venueCourt.getActive()
        );
    }

}
