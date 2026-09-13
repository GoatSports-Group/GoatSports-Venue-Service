package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.application.dto.response.address.AddressSuggestionCommand;

public record AddressSuggestionResponse(
        String id,
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
    public static AddressSuggestionResponse from(AddressSuggestionCommand command) {
        return new AddressSuggestionResponse(
                command.refId(),
                command.refId(),
                command.title(),
                command.formattedAddress(),
                command.address(),
                command.ward(),
                command.district(),
                command.city(),
                command.latitude(),
                command.longitude()
        );
    }
}
