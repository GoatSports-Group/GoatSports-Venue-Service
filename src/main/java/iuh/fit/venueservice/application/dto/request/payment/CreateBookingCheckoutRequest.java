package iuh.fit.venueservice.application.dto.request.payment;

import java.util.UUID;

public record CreateBookingCheckoutRequest(
        UUID bookingId,
        Double amount,
        String currency,
        String locale
) {
}
