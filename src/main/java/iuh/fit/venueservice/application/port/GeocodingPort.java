package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.response.address.GeocodingResultCommand;

import java.util.List;
import java.util.Optional;

public interface GeocodingPort {
    List<GeocodingResultCommand> search(String query);

    Optional<GeocodingResultCommand> reverse(double latitude, double longitude);
}
