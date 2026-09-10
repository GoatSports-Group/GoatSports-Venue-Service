package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.CheckInMethod;

import java.util.UUID;

public record ConfirmOwnerCheckInCommand(
        UUID ownerId,
        UUID bookingId,
        CheckInMethod method,
        PaymentCollectionMode paymentMode,
        String qrCode,
        String bookingCode
) {
    public void validate() throws ServiceException {
        if (ownerId == null || bookingId == null || method == null || paymentMode == null) {
            throw new ServiceException(
                    "Đơn đặt sân, phương thức nhận sân và cách thu tiền là bắt buộc."
            );
        }
    }
}
