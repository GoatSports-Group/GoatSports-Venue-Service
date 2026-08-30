package iuh.fit.venueservice.application.dto.request.payment;

import java.util.Set;
import java.util.UUID;

public record PaymentReferenceQueryRequest(
        String referenceType,
        Set<UUID> referenceIds
) {
}
