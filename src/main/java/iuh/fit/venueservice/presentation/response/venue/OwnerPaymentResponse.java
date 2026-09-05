package iuh.fit.venueservice.presentation.response.venue;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OwnerPaymentResponse(
        UUID paymentId,
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
}
