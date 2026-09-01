package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.BookingCheckoutSnapshot;

import java.util.UUID;
import java.time.LocalDateTime;

public interface PaymentCollectionPort {
    PaymentSnapshot collectBookingRemaining(
            UUID bookingId,
            UUID payerId,
            UUID collectorId,
            Double amount
    );

    BookingCheckoutSnapshot createBookingCheckout(
            UUID bookingId,
            UUID payerId,
            Double amount,
            LocalDateTime expiresAt
    );

}
