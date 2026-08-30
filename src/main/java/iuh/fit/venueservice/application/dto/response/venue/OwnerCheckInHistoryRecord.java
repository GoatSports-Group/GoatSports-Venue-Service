package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.CheckInRecord;

public record OwnerCheckInHistoryRecord(
        OwnerBookingRecord bookingRecord,
        CheckInRecord checkInRecord
) {
}
