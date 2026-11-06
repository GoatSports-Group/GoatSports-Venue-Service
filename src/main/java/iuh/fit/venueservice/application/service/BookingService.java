package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.*;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.PlayerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {

    List<TimeSlotResponseCommand> getAvailableSlots(UUID venueCourtId, LocalDate date) throws ServiceException;

    Booking createBooking(CreateBookingCommand command) throws ServiceException;

    Booking getBookingById(UUID bookingId) throws ServiceException;

    Booking getMatchmakingBooking(UUID matchmakingSessionId) throws ServiceException;

    PlayerBookingView getPlayerBooking(UUID bookingId, UUID playerId) throws ServiceException;

    List<Booking> getBookingHistory(UUID userId, String status, int page, int size);

    PageResult<PlayerBookingView> getPlayerBookingHistory(
            UUID playerId, String status, int page, int size
    ) throws ServiceException;

    String generateBookingCode();
}
