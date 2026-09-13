package iuh.fit.venueservice.application.dto.response.address;

public record GeocodingResultCommand(
        double latitude,
        double longitude,
        String title,
        String address
) {
}
