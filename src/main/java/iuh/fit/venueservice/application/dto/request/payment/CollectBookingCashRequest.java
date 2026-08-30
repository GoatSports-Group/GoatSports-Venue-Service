package iuh.fit.venueservice.application.dto.request.payment;

import java.util.UUID;

public record CollectBookingCashRequest(
        UUID payerId,
        UUID bookingId,
        Double amount,
        String currency
) {
}
