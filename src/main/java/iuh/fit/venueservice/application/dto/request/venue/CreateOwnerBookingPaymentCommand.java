package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record CreateOwnerBookingPaymentCommand(
        UUID bookingId,
        UUID ownerId,
        OwnerBookingPaymentMethod method
) {
    public void validate() throws ServiceException {
        if (bookingId == null || ownerId == null || method == null) {
            throw new ServiceException("Booking, owner và phương thức thanh toán là bắt buộc.");
        }
    }
}
