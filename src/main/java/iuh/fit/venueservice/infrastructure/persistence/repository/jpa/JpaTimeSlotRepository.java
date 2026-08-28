package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.TimeSlotEntity;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTimeSlotRepository extends JpaRepository<TimeSlotEntity, UUID> {
    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateOrderByStartTime(UUID venueCourtId, LocalDate date);

    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateAndStatus(UUID venueCourtId, LocalDate date, TimeSlotStatus status);
}
