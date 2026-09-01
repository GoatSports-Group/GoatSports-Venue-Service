package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RevenueBookingRecord(
        UUID bookingId,
        LocalDate playDate,
        LocalTime startTime
) {
}
