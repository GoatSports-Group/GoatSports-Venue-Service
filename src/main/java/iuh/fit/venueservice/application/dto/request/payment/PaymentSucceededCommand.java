package iuh.fit.venueservice.application.dto.request.payment;

import java.util.UUID;

public record PaymentSucceededCommand(
        UUID paymentId,
        UUID payerId,
        String purpose,
        String referenceType,
        UUID referenceId,
        Double amount,
        String currency,
        String status
) {
}
