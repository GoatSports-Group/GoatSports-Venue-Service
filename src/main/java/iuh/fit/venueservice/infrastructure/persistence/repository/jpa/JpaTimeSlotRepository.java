package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.TimeSlotEntity;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTimeSlotRepository extends JpaRepository<TimeSlotEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<TimeSlotEntity> findLockedByTimeSlotId(UUID timeSlotId);

    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateOrderByStartTime(UUID venueCourtId, LocalDate date);

    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateAndStatus(UUID venueCourtId, LocalDate date, TimeSlotStatus status);

    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateBetweenOrderByDateAscStartTimeAsc(
            UUID venueCourtId, LocalDate fromDate, LocalDate toDate
    );
}
