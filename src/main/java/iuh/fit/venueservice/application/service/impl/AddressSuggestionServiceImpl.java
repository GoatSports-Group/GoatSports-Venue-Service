package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.address.AddressSuggestionCommand;
import iuh.fit.venueservice.application.port.AddressSuggestionPort;
import iuh.fit.venueservice.application.service.AddressSuggestionService;

import java.util.List;
import java.util.Optional;

public class AddressSuggestionServiceImpl implements AddressSuggestionService {
    private final AddressSuggestionPort addressSuggestionPort;

    public AddressSuggestionServiceImpl(AddressSuggestionPort addressSuggestionPort) {
        this.addressSuggestionPort = addressSuggestionPort;
    }

    @Override
    public List<AddressSuggestionCommand> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 3 || normalizedQuery.length() > 160) {
            return List.of();
        }
        return addressSuggestionPort.search(normalizedQuery);
    }

    @Override
    public Optional<AddressSuggestionCommand> resolve(String refId) {
        String normalizedRefId = refId == null ? "" : refId.trim();
        if (normalizedRefId.length() > 2048
                || !(normalizedRefId.startsWith("auto:") || normalizedRefId.startsWith("geocode:"))) {
            return Optional.empty();
        }
        return addressSuggestionPort.resolve(normalizedRefId);
    }
}
