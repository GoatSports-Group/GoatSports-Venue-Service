package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;

import java.util.UUID;

public interface PaymentCollectionPort {
    PaymentSnapshot collectBookingRemaining(
            UUID bookingId,
            UUID payerId,
            UUID collectorId,
            Double amount
    );
}
