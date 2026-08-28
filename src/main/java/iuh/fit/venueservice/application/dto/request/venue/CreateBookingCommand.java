package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.BookingSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateBookingCommand(
        UUID playerId,
        UUID venueCourtId,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        BookingSource source,
        String walkInCustomerName,
        String walkInCustomerPhone,
        UUID matchmakingSessionId
) {}
