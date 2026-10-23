package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.application.dto.response.address.GeocodingResultCommand;
import iuh.fit.venueservice.domain.entity.GeoBounds;

public record GeocodingResponse(
        double latitude,
        double longitude,
        String title,
        String address,
        Bounds bounds
) {
    /** Khung nam - tây - bắc - đông; null khi Nominatim không trả. */
    public record Bounds(double south, double west, double north, double east) {}

    public static GeocodingResponse from(GeocodingResultCommand command) {
        GeoBounds bounds = command.bounds();
        return new GeocodingResponse(
                command.latitude(),
                command.longitude(),
                command.title(),
                command.address(),
                bounds == null ? null : new Bounds(bounds.south(), bounds.west(), bounds.north(), bounds.east())
        );
    }
}
