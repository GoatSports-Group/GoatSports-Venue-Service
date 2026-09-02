package iuh.fit.venueservice.domain.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VenueFacilityLayout(
        int version,
        UUID venueId,
        List<Item> items,
        List<Zone> zones,
        LocalDateTime updatedAt
) {
    public record Item(
            String id,
            String type,
            UUID courtId,
            String label,
            double x,
            double y,
            double width,
            double height,
            double rotation,
            String zoneId,
            String icon
    ) { }

    public record Zone(
            String id,
            String name,
            double x,
            double y,
            double width,
            double height
    ) { }
}
