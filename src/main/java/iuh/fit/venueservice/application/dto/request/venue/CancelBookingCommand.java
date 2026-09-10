package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record CancelBookingCommand(
        UUID bookingId,
        UUID requestedByUserId,
        String reason
) {
    public void validate() throws ServiceException {
        if (bookingId == null || requestedByUserId == null || reason == null || reason.isBlank()) {
            throw new ServiceException("Booking, người yêu cầu và lý do hủy là bắt buộc.");
        }
        if (reason.trim().length() > 1000) {
            throw new ServiceException("Lý do hủy không được vượt quá 1000 ký tự.");
        }
    }
}
