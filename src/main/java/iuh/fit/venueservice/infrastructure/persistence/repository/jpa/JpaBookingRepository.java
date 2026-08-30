package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.BookingEntity;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface JpaBookingRepository extends JpaRepository<BookingEntity, UUID>, JpaSpecificationExecutor<BookingEntity> {
    Page<BookingEntity> findByPlayerIdOrderByCreatedAtDesc(UUID playerId, Pageable pageable);

    Page<BookingEntity> findByPlayerIdAndStatusOrderByCreatedAtDesc(UUID playerId, BookingStatus status, Pageable pageable);

    Optional<BookingEntity> findByBookingCode(String bookingCode);

    Optional<BookingEntity> findByQrCode(String qrCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking from BookingEntity booking
            join fetch booking.venueCourt court
            join fetch court.venue venue
            where booking.bookingId = :bookingId and venue.ownerId = :ownerId
            """)
    Optional<BookingEntity> findOwnedByIdForUpdate(
            @Param("bookingId") UUID bookingId,
            @Param("ownerId") UUID ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from BookingEntity booking where booking.bookingId = :bookingId")
    Optional<BookingEntity> findByIdForUpdate(@Param("bookingId") UUID bookingId);

    @Query("""
            select booking from BookingEntity booking
            join fetch booking.venueCourt court
            join fetch court.venue venue
            where venue.ownerId = :ownerId
              and (:venueId is null or venue.venueId = :venueId)
              and booking.playDate between :fromDate and :toDate
            order by booking.playDate, booking.startTime, booking.bookingId
            """)
    List<BookingEntity> findOwnedForRevenue(
            @Param("ownerId") UUID ownerId,
            @Param("venueId") UUID venueId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
