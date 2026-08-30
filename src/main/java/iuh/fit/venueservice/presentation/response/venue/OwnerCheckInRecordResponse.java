package iuh.fit.venueservice.presentation.response.venue;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OwnerCheckInRecordResponse(
        UUID checkInId,
        UUID paymentId,
        UUID checkedInBy,
        String method,
        BigDecimal remainingAmountCollected,
        LocalDateTime checkedInAt
) {
}
