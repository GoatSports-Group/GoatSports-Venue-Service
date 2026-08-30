package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.util.UUID;

public record OwnerBookingFilter(
        UUID ownerId,
        UUID venueId,
        UUID venueCourtId,
        BookingStatus status,
        String query,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
) {
}
