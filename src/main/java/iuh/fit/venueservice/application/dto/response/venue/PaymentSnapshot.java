package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentSnapshot(
        UUID paymentId,
        UUID referenceId,
        String purpose,
        Double amount,
        String currency,
        String status,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
