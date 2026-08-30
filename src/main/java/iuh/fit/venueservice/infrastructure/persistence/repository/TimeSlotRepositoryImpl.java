package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import iuh.fit.venueservice.infrastructure.persistence.mapper.TimeSlotEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaTimeSlotRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TimeSlotRepositoryImpl implements TimeSlotRepository {
    private final JpaTimeSlotRepository jpaRepository;
    private final TimeSlotEntityMapper mapper;
    private final JpaVenueCourtRepository jpaVenueCourtRepository;

    @Override
    public TimeSlot save(TimeSlot timeSlot) {
        var entity = mapper.toEntity(timeSlot);
        entity.setVenueCourt(jpaVenueCourtRepository.getReferenceById(timeSlot.getVenueCourtId()));
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TimeSlot> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TimeSlot> findByVenueCourtIdAndDate(UUID venueCourtId, LocalDate date) {
        return jpaRepository.findByVenueCourt_VenueCourtIdAndDateOrderByStartTime(venueCourtId, date)
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TimeSlot> findAvailableSlots(UUID venueCourtId, LocalDate date) {
        return jpaRepository.findByVenueCourt_VenueCourtIdAndDateAndStatus(venueCourtId, date, TimeSlotStatus.AVAILABLE)
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TimeSlot> findByVenueCourtIdAndDateBetween(
            UUID venueCourtId, LocalDate fromDate, LocalDate toDate
    ) {
        return jpaRepository
                .findByVenueCourt_VenueCourtIdAndDateBetweenOrderByDateAscStartTimeAsc(
                        venueCourtId, fromDate, toDate
                )
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TimeSlot> saveAll(List<TimeSlot> timeSlots) {
        var entities = timeSlots.stream().map(timeSlot -> {
            var entity = mapper.toEntity(timeSlot);
            entity.setVenueCourt(jpaVenueCourtRepository.getReferenceById(timeSlot.getVenueCourtId()));
            return entity;
        }).collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID timeSlotId) {
        jpaRepository.deleteById(timeSlotId);
    }
}
