package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDate;
import java.util.UUID;

public record RevenueBookingRecord(
        UUID bookingId,
        LocalDate playDate
) {
}
