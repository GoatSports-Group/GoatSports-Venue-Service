package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.response.address.AddressSuggestionCommand;

import java.util.List;
import java.util.Optional;

public interface AddressSuggestionPort {
    List<AddressSuggestionCommand> search(String query);

    Optional<AddressSuggestionCommand> resolve(String refId);
}
