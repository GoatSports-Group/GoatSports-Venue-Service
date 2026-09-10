package iuh.fit.venueservice.application.dto.request.payment;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record PaymentTerminalCommand(
        UUID paymentId,
        UUID payerId,
        String purpose,
        String referenceType,
        UUID referenceId,
        String status,
        String failureCode,
        String failureReason
) {
    public boolean isBookingDepositTerminal() {
        return "BOOKING".equals(referenceType)
                && "BOOKING_DEPOSIT".equals(purpose)
                && ("FAILED".equals(status) || "CANCELLED".equals(status) || "EXPIRED".equals(status));
    }

    public void validateIdentity() throws ServiceException {
        if (paymentId == null || referenceId == null || payerId == null) {
            throw new ServiceException("Payment terminal event thiếu định danh bắt buộc.");
        }
    }
}
