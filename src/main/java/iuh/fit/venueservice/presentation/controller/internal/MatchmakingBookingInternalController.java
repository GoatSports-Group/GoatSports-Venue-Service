package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.presentation.response.internal.MatchmakingBookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/matchmaking/bookings")
@RequiredArgsConstructor
public class MatchmakingBookingInternalController {
    private final BookingService bookingService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<MatchmakingBookingResponse> getBySessionId(
            @PathVariable UUID sessionId
    ) throws ServiceException {
        Booking booking = bookingService.getMatchmakingBooking(sessionId);
        return ResponseEntity.ok(new MatchmakingBookingResponse(
                booking.getBookingId(),
                booking.getMatchmakingSessionId(),
                booking.getPlayerId(),
                booking.getVenueCourtId(),
                booking.getStatus(),
                booking.getPlayDate(),
                booking.getStartTime(),
                booking.getEndTime()
        ));
    }
}
