package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.application.dto.response.address.GeocodingResultCommand;

public record GeocodingResponse(
        double latitude,
        double longitude,
        String title,
        String address
) {
    public static GeocodingResponse from(GeocodingResultCommand command) {
        return new GeocodingResponse(
                command.latitude(),
                command.longitude(),
                command.title(),
                command.address()
        );
    }
}
