package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.address.GeocodingResultCommand;
import iuh.fit.venueservice.application.port.GeocodingPort;
import iuh.fit.venueservice.application.service.GeocodingService;

import java.util.List;
import java.util.Optional;

public class GeocodingServiceImpl implements GeocodingService {
    private final GeocodingPort geocodingPort;

    public GeocodingServiceImpl(GeocodingPort geocodingPort) {
        this.geocodingPort = geocodingPort;
    }

    @Override
    public List<GeocodingResultCommand> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 3) {
            return List.of();
        }
        return geocodingPort.search(normalizedQuery);
    }

    @Override
    public Optional<GeocodingResultCommand> reverse(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return Optional.empty();
        }
        return geocodingPort.reverse(latitude, longitude);
    }
}
