package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.TimeSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository {
    TimeSlot save(TimeSlot timeSlot);

    Optional<TimeSlot> findById(UUID id);

    Optional<TimeSlot> findByIdForUpdate(UUID id);

    List<TimeSlot> findByVenueCourtIdAndDate(UUID venueCourtId, LocalDate date);

    List<TimeSlot> findAvailableSlots(UUID venueCourtId, LocalDate date);

    List<TimeSlot> findByVenueCourtIdAndDateBetween(
            UUID venueCourtId, LocalDate fromDate, LocalDate toDate
    );

    List<TimeSlot> saveAll(List<TimeSlot> timeSlots);

    void deleteById(UUID timeSlotId);
}
