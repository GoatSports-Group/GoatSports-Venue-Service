package iuh.fit.venueservice.application.dto.request.payment;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record PaymentSucceededCommand(
        UUID paymentId,
        UUID payerId,
        String purpose,
        String referenceType,
        UUID referenceId,
        Double amount,
        String currency,
        String status
) {
    public boolean isBookingPaymentSucceeded() {
        return "SUCCEEDED".equals(status) && "BOOKING".equals(referenceType);
    }

    public void validateIdentity() throws ServiceException {
        if (paymentId == null || referenceId == null || payerId == null) {
            throw new ServiceException("Payment event thiếu định danh bắt buộc.");
        }
        if (!"VND".equals(currency)) {
            throw new ServiceException("Currency của payment event không hợp lệ.");
        }
    }
}
