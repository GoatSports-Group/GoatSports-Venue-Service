package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record OwnerCheckInLookupCommand(
        UUID ownerId,
        UUID bookingId,
        String qrCode,
        String bookingCode
) {
    public void validate() throws ServiceException {
        if (ownerId == null) {
            throw new ServiceException("Không xác định được chủ sân.");
        }
        int identifiers = (bookingId == null ? 0 : 1) + hasText(qrCode) + hasText(bookingCode);
        if (identifiers != 1) {
            throw new ServiceException("Cần cung cấp đúng một mã QR, mã đặt sân hoặc mã định danh đơn.");
        }
    }

    private int hasText(String value) {
        return value != null && !value.isBlank() ? 1 : 0;
    }
}
