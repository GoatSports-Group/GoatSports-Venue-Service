package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.BookingCancellationEntity;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface JpaBookingCancellationRepository extends JpaRepository<BookingCancellationEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cancellation from BookingCancellationEntity cancellation "
            + "where cancellation.cancellationId = :cancellationId")
    Optional<BookingCancellationEntity> findByIdForUpdate(UUID cancellationId);

    Optional<BookingCancellationEntity> findByBooking_BookingId(UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cancellation from BookingCancellationEntity cancellation where cancellation.refundId = :refundId")
    Optional<BookingCancellationEntity> findByRefundIdForUpdate(UUID refundId);

    Page<BookingCancellationEntity> findByStatusOrderByCreatedAtDesc(CancellationStatus status, Pageable pageable);

    List<BookingCancellationEntity> findByBooking_BookingIdIn(Set<UUID> bookingIds);
}
