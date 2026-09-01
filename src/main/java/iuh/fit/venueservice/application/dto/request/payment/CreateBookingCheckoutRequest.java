package iuh.fit.venueservice.application.dto.request.payment;

import java.util.UUID;
import java.time.LocalDateTime;

public record CreateBookingCheckoutRequest(
        UUID bookingId,
        Double amount,
        String currency,
        String locale,
        LocalDateTime expiresAt
) {
}
