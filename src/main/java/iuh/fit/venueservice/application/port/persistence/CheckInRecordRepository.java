package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.CheckInRecord;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCheckInHistoryRecord;

import java.util.Optional;
import java.util.UUID;

public interface CheckInRecordRepository {
    CheckInRecord save(CheckInRecord checkInRecord);

    Optional<CheckInRecord> findByBookingId(UUID bookingId);

    PageResult<OwnerCheckInHistoryRecord> findOwnedCheckIns(OwnerCheckInFilter filter);
}
