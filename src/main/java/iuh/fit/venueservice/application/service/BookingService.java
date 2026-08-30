package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.*;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingService {

    List<TimeSlotResponseCommand> getAvailableSlots(UUID venueCourtId, LocalDate date) throws ServiceException;

    Booking createBooking(CreateBookingCommand command) throws ServiceException;

    Booking getBookingById(UUID bookingId) throws ServiceException;

    List<Booking> getBookingHistory(UUID userId, String status, int page, int size);

    BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException;

    BookingCancellation processCancellation(UUID cancellationId, boolean approved, UUID processedByUserId, String processNote) throws ServiceException;

    String generateBookingCode();
}
