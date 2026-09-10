package iuh.fit.venueservice.application.dto.request.payment;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record RefundResultCommand(
        UUID refundId,
        UUID paymentId,
        String status,
        String failureReason
) {
    public void validateIdentity() throws ServiceException {
        if (refundId == null || paymentId == null) {
            throw new ServiceException("Refund event thiếu định danh bắt buộc.");
        }
    }
}
