package iuh.fit.venueservice.presentation.response.venue;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponse {
    private UUID timeSlotId;
    private UUID venueCourtId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double pricePerHour;
    private String status;
}
