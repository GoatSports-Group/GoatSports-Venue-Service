package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.BookingEntity;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBookingRepository extends JpaRepository<BookingEntity, UUID>, JpaSpecificationExecutor<BookingEntity> {
    Page<BookingEntity> findByPlayerIdOrderByCreatedAtDesc(UUID playerId, Pageable pageable);

    Page<BookingEntity> findByPlayerIdAndStatusOrderByCreatedAtDesc(UUID playerId, BookingStatus status, Pageable pageable);

    Optional<BookingEntity> findByBookingCode(String bookingCode);

    Optional<BookingEntity> findByQrCode(String qrCode);
}
