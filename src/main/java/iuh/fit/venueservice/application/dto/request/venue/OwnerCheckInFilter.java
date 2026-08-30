package iuh.fit.venueservice.application.dto.request.venue;

import java.time.LocalDate;
import java.util.UUID;

public record OwnerCheckInFilter(
        UUID ownerId,
        UUID venueId,
        UUID venueCourtId,
        LocalDate date,
        int page,
        int size
) {
}
