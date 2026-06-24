package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.venue.VenueResponseCommand;
import iuh.fit.venueservice.presentation.response.venue.VenueResponse;

public class VenueConverter {

    public static VenueResponse toResponse(VenueResponseCommand venue) {
        if (venue == null) return null;
        return VenueResponse.builder()
                .venueId(venue.venueId())
                .name(venue.name())
                .description(venue.description())
                .openTime(venue.openTime())
                .closeTime(venue.closeTime())
                .active(venue.active())
                .minPrice(venue.minPrice())
                .maxPrice(venue.maxPrice())
                .averageRating(venue.averageRating())
                .totalReviews(venue.totalReviews())
                .phone(venue.phone())
                .email(venue.email())
                .build();
    }

}
