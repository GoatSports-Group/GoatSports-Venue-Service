package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.CheckInRecordRepository;
import iuh.fit.venueservice.domain.entity.CheckInRecord;
import iuh.fit.venueservice.infrastructure.persistence.mapper.CheckInRecordEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaCheckInRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckInRecordRepositoryImpl implements CheckInRecordRepository {
    private final JpaCheckInRecordRepository jpaRepository;
    private final JpaBookingRepository jpaBookingRepository;
    private final CheckInRecordEntityMapper mapper;

    @Override
    public CheckInRecord save(CheckInRecord checkInRecord) {
        var entity = mapper.toEntity(checkInRecord);
        entity.setBooking(jpaBookingRepository.getReferenceById(checkInRecord.getBookingId()));
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
