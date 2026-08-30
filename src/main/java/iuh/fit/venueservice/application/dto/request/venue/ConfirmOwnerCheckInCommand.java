package iuh.fit.venueservice.application.dto.request.venue;

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
}
