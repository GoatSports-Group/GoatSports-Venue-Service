package iuh.fit.venueservice.application.dto.request.payment;

import java.util.UUID;

public record PaymentTerminalCommand(
        UUID paymentId,
        UUID payerId,
        String purpose,
        String referenceType,
        UUID referenceId,
        String status,
        String failureCode,
        String failureReason
) {
}
