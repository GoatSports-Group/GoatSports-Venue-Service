package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.application.service.impl.BookingServiceImpl;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class BookingDecorator implements BookingService {
    private final BookingServiceImpl delegate;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponseCommand> getAvailableSlots(UUID venueCourtId, LocalDate date)
            throws ServiceException {
        return delegate.getAvailableSlots(venueCourtId, date);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public Booking createBooking(CreateBookingCommand command) throws ServiceException {
        return delegate.createBooking(command);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getBookingById(UUID bookingId) throws ServiceException {
        return delegate.getBookingById(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingHistory(UUID userId, String status, int page, int size) {
        return delegate.getBookingHistory(userId, status, page, size);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException {
        return delegate.requestCancellation(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public BookingCancellation processCancellation(
            UUID cancellationId, boolean approved, UUID processedByUserId, String processNote
    ) throws ServiceException {
        return delegate.processCancellation(cancellationId, approved, processedByUserId, processNote);
    }

    @Override
    public String generateBookingCode() {
        return delegate.generateBookingCode();
    }
}
