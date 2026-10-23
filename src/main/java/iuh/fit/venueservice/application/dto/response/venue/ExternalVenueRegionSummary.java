package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDateTime;

public record ExternalVenueRegionSummary(String region, long venueCount, LocalDateTime lastImportedAt) {}
