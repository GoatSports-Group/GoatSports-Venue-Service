package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCustomerMetricsRecord;
import iuh.fit.venueservice.application.dto.response.venue.RevenueBookingRecord;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(UUID id);

    Optional<Booking> findByIdForUpdate(UUID id);

    Optional<Booking> findByBookingCode(String bookingCode);

    Optional<Booking> findByQrCode(String qrCode);

    Optional<Booking> findLatestByMatchmakingSessionId(UUID matchmakingSessionId);

    List<Booking> findByPlayerId(UUID playerId, int page, int size);

    List<Booking> findByPlayerIdAndStatus(UUID playerId, String status, int page, int size);

    PageResult<Booking> findPlayerPage(UUID playerId, BookingStatus status, int page, int size);

    long countByPlayerId(UUID playerId);

    List<Booking> findExpiredPaymentHoldsForUpdate(LocalDateTime cutoff);

    List<Booking> findCompletionCandidatesForUpdate(LocalDateTime cutoff, int limit);

    PageResult<OwnerBookingRecord> findOwnedBookings(OwnerBookingFilter filter);

    Optional<OwnerBookingRecord> findOwnedBookingById(UUID bookingId, UUID ownerId);

    Optional<OwnerBookingRecord> findOwnedBookingByIdForUpdate(UUID bookingId, UUID ownerId);

    List<RevenueBookingRecord> findOwnedRevenueBookings(
            UUID ownerId, UUID venueId, LocalDate fromDate, LocalDate toDate
    );

    OwnerCustomerMetricsRecord findOwnedCustomerMetrics(
            UUID ownerId,
            UUID venueId,
            LocalDate previousFrom,
            LocalDate currentFrom,
            LocalDate currentToExclusive
    );
}
