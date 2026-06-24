package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.venue.VenueResponseCommand;
import iuh.fit.venueservice.domain.entity.Venue;

public class VenueMapper {

    public static VenueResponseCommand toVenueResponseCommand(Venue venue) {
        return new VenueResponseCommand(
                venue.getVenueId(),
                venue.getName(),
                venue.getDescription(),
                venue.getOpenTime(),
                venue.getCloseTime(),
                venue.getActive(),
                venue.getMinPrice(),
                venue.getMaxPrice(),
                venue.getAverageRating(),
                venue.getTotalReviews(),
                venue.getPhone(),
                venue.getEmail()
        );
    }

}
