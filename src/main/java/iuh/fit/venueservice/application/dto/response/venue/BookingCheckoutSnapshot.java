package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingCheckoutSnapshot(
        UUID paymentId,
        String status,
        String checkoutUrl,
        String deeplink,
        String qrCodeContent,
        LocalDateTime expiresAt
) {
}
