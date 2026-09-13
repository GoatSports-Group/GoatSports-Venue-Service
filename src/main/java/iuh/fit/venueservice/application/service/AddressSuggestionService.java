package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.response.address.AddressSuggestionCommand;

import java.util.List;
import java.util.Optional;

public interface AddressSuggestionService {
    List<AddressSuggestionCommand> search(String query);

    Optional<AddressSuggestionCommand> resolve(String refId);
}
