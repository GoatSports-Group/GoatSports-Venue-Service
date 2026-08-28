package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.CheckInRecord;

public interface CheckInRecordRepository {
    CheckInRecord save(CheckInRecord checkInRecord);
}
