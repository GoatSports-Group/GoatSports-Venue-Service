package iuh.fit.venueservice.application.dto.request.venue;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateTimeSlotsCommand(
        UUID venueCourtId,
        LocalDate fromDate,
        LocalDate toDate,
        Integer slotDurationMinutes,
        UUID currentUserId
) {}
