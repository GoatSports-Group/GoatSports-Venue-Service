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
        String provider,
        String method,
        String providerTransactionId,
        LocalDateTime paidAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public PaymentSnapshot(
            UUID paymentId,
            UUID referenceId,
            String purpose,
            Double amount,
            String currency,
            String status,
            String provider,
            String method,
            LocalDateTime paidAt,
            LocalDateTime expiresAt,
            LocalDateTime createdAt
    ) {
        this(paymentId, referenceId, purpose, amount, currency, status, provider, method, null,
                paidAt, expiresAt, createdAt);
    }

    public PaymentSnapshot(
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
        this(paymentId, referenceId, purpose, amount, currency, status, null, null, null,
                paidAt, expiresAt, createdAt);
    }
}
