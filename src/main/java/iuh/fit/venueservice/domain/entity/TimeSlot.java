package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TimeSlot extends BaseEntity {
    private UUID timeSlotId;
    private UUID venueCourtId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private TimeSlotStatus status;
    private Double pricePerHour;
}
