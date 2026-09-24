package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.TimeSlotEntity;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.time.LocalTime;
import java.util.Set;
import java.util.Optional;

@Repository
public interface JpaTimeSlotRepository extends JpaRepository<TimeSlotEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TimeSlotEntity> findLockedByTimeSlotId(UUID timeSlotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select slot from TimeSlotEntity slot
            where slot.venueCourt.venueCourtId = :courtId
              and slot.date = :date
              and slot.startTime = :startTime
              and slot.endTime = :endTime
            """)
    Optional<TimeSlotEntity> findByCourtAndTimeForUpdate(
            @Param("courtId") UUID courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateOrderByStartTime(UUID venueCourtId, LocalDate date);

    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateAndStatus(UUID venueCourtId, LocalDate date, TimeSlotStatus status);

    @Query("""
            select slot from TimeSlotEntity slot
            where slot.venueCourt.venueCourtId in :courtIds
              and slot.date = :date
              and slot.startTime <= :time
              and slot.endTime > :time
              and slot.status in :statuses
            order by slot.endTime asc
            """)
    List<TimeSlotEntity> findBlockingSlotsAt(
            @Param("courtIds") Set<UUID> courtIds,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            @Param("statuses") Set<TimeSlotStatus> statuses
    );

    List<TimeSlotEntity> findByVenueCourt_VenueCourtIdAndDateBetweenOrderByDateAscStartTimeAsc(
            UUID venueCourtId, LocalDate fromDate, LocalDate toDate
    );

    /** Khoa moi slot cua cac san trong khoang ngay: giu san cho giai va dat san thuong xep hang tai day. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select slot from TimeSlotEntity slot
            where slot.venueCourt.venueCourtId in :courtIds
              and slot.date between :fromDate and :toDate
            order by slot.date asc, slot.startTime asc
            """)
    List<TimeSlotEntity> findByCourtsAndDateRangeForUpdate(
            @Param("courtIds") java.util.Collection<UUID> courtIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
