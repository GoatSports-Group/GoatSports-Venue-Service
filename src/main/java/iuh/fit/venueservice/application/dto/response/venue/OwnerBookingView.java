package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.util.List;
import java.util.UUID;

public record OwnerBookingView(
        Booking booking,
        UUID venueId,
        String venueName,
        String courtName,
        BookingCancellation cancellation,
        List<PaymentSnapshot> payments,
        List<BookingStatus> allowedTransitions
) {
}
