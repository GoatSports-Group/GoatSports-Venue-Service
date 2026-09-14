package iuh.fit.venueservice.presentation.response.internal;

import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record MatchmakingBookingResponse(
        UUID bookingId,
        UUID matchmakingSessionId,
        UUID playerId,
        UUID venueCourtId,
        BookingStatus status,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
