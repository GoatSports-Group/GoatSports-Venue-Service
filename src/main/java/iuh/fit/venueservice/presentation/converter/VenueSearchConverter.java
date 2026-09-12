package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.venue.PublicReviewView;
import iuh.fit.venueservice.application.dto.response.venue.VenueSearchResponseCommand;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.presentation.response.venue.CancellationPolicyResponse;
import iuh.fit.venueservice.presentation.response.venue.PublicReviewResponse;
import iuh.fit.venueservice.presentation.response.venue.VenueDetailResponse;
import iuh.fit.venueservice.presentation.response.venue.VenueSearchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VenueCourtConverter.class})
public interface VenueSearchConverter {
    VenueSearchResponse toResponse(VenueSearchResponseCommand command);

    @Mapping(target = "courts", source = "courts")
    @Mapping(target = "address", source = "address.address")
    @Mapping(target = "ward", source = "address.ward")
    @Mapping(target = "district", source = "address.district")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "latitude", source = "address.latitude")
    @Mapping(target = "longitude", source = "address.longitude")
    VenueDetailResponse toDetailResponse(Venue venue);

    CancellationPolicyResponse toResponse(CancellationPolicy policy);

    PublicReviewResponse toResponse(PublicReviewView review);
}
