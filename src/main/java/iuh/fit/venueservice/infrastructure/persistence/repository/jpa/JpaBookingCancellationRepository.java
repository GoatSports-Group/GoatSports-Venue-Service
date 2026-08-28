package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.BookingCancellationEntity;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBookingCancellationRepository extends JpaRepository<BookingCancellationEntity, UUID> {
    Optional<BookingCancellationEntity> findByBooking_BookingId(UUID bookingId);

    Page<BookingCancellationEntity> findByStatusOrderByCreatedAtDesc(CancellationStatus status, Pageable pageable);
}
