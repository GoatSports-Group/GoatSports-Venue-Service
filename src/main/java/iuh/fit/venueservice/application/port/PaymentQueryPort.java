package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PaymentQueryPort {
    List<PaymentSnapshot> findByBookingIds(Set<UUID> bookingIds);
}
