package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.BookingCancellation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingCancellationRepository {
    BookingCancellation save(BookingCancellation cancellation);

    Optional<BookingCancellation> findById(UUID id);

    Optional<BookingCancellation> findByBookingId(UUID bookingId);

    List<BookingCancellation> findByStatus(String status, int page, int size);
}
