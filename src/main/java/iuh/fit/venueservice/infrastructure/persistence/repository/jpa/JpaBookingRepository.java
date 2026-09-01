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
import java.time.LocalDateTime;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking from BookingEntity booking
            where booking.status = iuh.fit.venueservice.domain.enums.BookingStatus.PENDING_PAYMENT
              and booking.holdExpiresAt is not null
              and booking.holdExpiresAt <= :cutoff
            order by booking.holdExpiresAt asc
            """)
    List<BookingEntity> findExpiredPaymentHoldsForUpdate(@Param("cutoff") LocalDateTime cutoff);

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

    @Query(value = """
            WITH scoped_bookings AS (
                SELECT CASE
                           WHEN booking.player_id IS NOT NULL
                               THEN 'PLAYER:' || CAST(booking.player_id AS text)
                           ELSE 'PHONE:' || NULLIF(
                               regexp_replace(COALESCE(booking.walk_in_customer_phone, ''), '[^0-9+]', '', 'g'),
                               ''
                           )
                       END AS customer_key,
                       booking.play_date
                FROM bookings booking
                JOIN venue_courts court ON court.venue_court_id = booking.venue_court_id
                JOIN venues venue ON venue.venue_id = court.venue_id
                WHERE venue.owner_id = :ownerId
                  AND venue.venue_id = :venueId
                  AND booking.status IN ('CONFIRMED', 'CHECKED_IN', 'COMPLETED')
                  AND booking.play_date < :currentToExclusive
            ),
            customer_history AS (
                SELECT customer_key,
                       MIN(play_date) AS first_play_date,
                       BOOL_OR(play_date >= :currentFrom) AS active_in_current,
                       BOOL_OR(play_date >= :previousFrom AND play_date < :currentFrom) AS active_in_previous
                FROM scoped_bookings
                WHERE customer_key IS NOT NULL
                GROUP BY customer_key
            )
            SELECT COUNT(*) FILTER (WHERE active_in_current) AS "currentTotalCustomers",
                   COUNT(*) FILTER (
                       WHERE active_in_current AND first_play_date >= :currentFrom
                   ) AS "currentNewCustomers",
                   COUNT(*) FILTER (
                       WHERE active_in_current AND first_play_date < :currentFrom
                   ) AS "currentReturningCustomers",
                   COUNT(*) FILTER (WHERE active_in_previous) AS "previousTotalCustomers",
                   COUNT(*) FILTER (
                       WHERE active_in_previous AND first_play_date >= :previousFrom
                   ) AS "previousNewCustomers",
                   COUNT(*) FILTER (
                       WHERE active_in_previous AND first_play_date < :previousFrom
                   ) AS "previousReturningCustomers"
            FROM customer_history
            """, nativeQuery = true)
    OwnerCustomerMetricsProjection findOwnedCustomerMetrics(
            @Param("ownerId") UUID ownerId,
            @Param("venueId") UUID venueId,
            @Param("previousFrom") LocalDate previousFrom,
            @Param("currentFrom") LocalDate currentFrom,
            @Param("currentToExclusive") LocalDate currentToExclusive
    );
}
