package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.CheckInRecord;

import java.math.BigDecimal;
import java.util.List;

public record OwnerCheckInView(
        OwnerBookingRecord bookingRecord,
        List<PaymentSnapshot> payments,
        CheckInRecord checkInRecord,
        BigDecimal outstandingAmount,
        boolean checkInEligible,
        String checkInMessage
) {
}
