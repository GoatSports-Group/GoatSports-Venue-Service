package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByBookingCode(String bookingCode);

    Optional<Booking> findByQrCode(String qrCode);

    List<Booking> findByPlayerId(UUID playerId, int page, int size);

    List<Booking> findByPlayerIdAndStatus(UUID playerId, String status, int page, int size);

    long countByPlayerId(UUID playerId);
}
