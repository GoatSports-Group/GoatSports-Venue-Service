package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TimeSlotResponseCommand(
        UUID timeSlotId,
        UUID venueCourtId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Double pricePerHour,
        String status
) {}
