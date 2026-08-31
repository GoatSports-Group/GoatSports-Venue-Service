package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingPaymentMethod;

import java.time.LocalDateTime;
import java.util.UUID;

public record OwnerBookingPaymentResult(
        UUID bookingId,
        UUID paymentId,
        OwnerBookingPaymentMethod method,
        String status,
        String checkoutUrl,
        LocalDateTime expiresAt
) {
}
