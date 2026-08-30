package iuh.fit.venueservice.presentation.response.venue;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OwnerCheckInResponse(
        OwnerBookingResponse booking,
        OwnerCheckInRecordResponse checkIn,
        BigDecimal outstandingAmount
) {
}
