package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.CheckInRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaCheckInRecordRepository extends JpaRepository<CheckInRecordEntity, UUID>,
        JpaSpecificationExecutor<CheckInRecordEntity> {
    java.util.Optional<CheckInRecordEntity> findByBooking_BookingId(UUID bookingId);
}
