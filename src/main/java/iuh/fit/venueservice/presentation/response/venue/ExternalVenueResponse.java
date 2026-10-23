package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.enums.SportType;

import java.util.List;
import java.util.UUID;

/** Sân ngoài hệ thống: không có giá, khung giờ hay đặt online; {@code source} để client ghi nguồn dữ liệu. */
public record ExternalVenueResponse(
        UUID externalVenueId,
        String name,
        List<SportType> sportTypes,
        double latitude,
        double longitude,
        String address,
        String phone,
        String website,
        String openingHours,
        String source,
        UUID claimedVenueId
) {
    public static ExternalVenueResponse from(ExternalVenue venue) {
        return new ExternalVenueResponse(
                venue.getExternalVenueId(),
                venue.getName(),
                venue.getSportTypes().stream().sorted().toList(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getAddress(),
                venue.getPhone(),
                venue.getWebsite(),
                venue.getOpeningHours(),
                venue.getSource(),
                venue.getClaimedVenueId()
        );
    }
}
