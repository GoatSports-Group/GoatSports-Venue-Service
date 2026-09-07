package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;

import java.util.UUID;

public record PlayerBookingView(
        Booking booking,
        UUID venueId,
        String venueName,
        String courtName,
        CancellationPolicy cancellationPolicy,
        BookingCancellation cancellation,
        PlayerReviewView review
) {
}
