package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.response.address.GeocodingResultCommand;

import java.util.List;
import java.util.Optional;

public interface GeocodingService {
    List<GeocodingResultCommand> search(String query);

    Optional<GeocodingResultCommand> reverse(double latitude, double longitude);
}
