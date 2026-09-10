package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.util.UUID;

public record UpdateOwnerBookingStatusCommand(
        UUID bookingId,
        BookingStatus status,
        UUID ownerId
) {
    public void validate() throws ServiceException {
        if (bookingId == null || ownerId == null || status == null) {
            throw new ServiceException("Booking, trạng thái và owner là bắt buộc.");
        }
    }
}
