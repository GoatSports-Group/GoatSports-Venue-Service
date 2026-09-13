package iuh.fit.venueservice.application.dto.response.address;

public record AddressSuggestionCommand(
        String refId,
        String title,
        String formattedAddress,
        String address,
        String ward,
        String district,
        String city,
        Double latitude,
        Double longitude
) {
}
