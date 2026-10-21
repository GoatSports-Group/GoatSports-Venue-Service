package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.enums.CancellationStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BookingCancellationRepository {
    BookingCancellation save(BookingCancellation cancellation);

    Optional<BookingCancellation> findById(UUID id);

    Optional<BookingCancellation> findByIdForUpdate(UUID id);

    Optional<BookingCancellation> findByBookingId(UUID bookingId);

    Optional<BookingCancellation> findByRefundIdForUpdate(UUID refundId);

    List<BookingCancellation> findByStatus(String status, int page, int size);

    Map<UUID, BookingCancellation> findByBookingIds(Set<UUID> bookingIds);

    /** Yêu cầu hủy ở các trạng thái cho trước mà chưa có process Camunda, cũ nhất trước. */
    List<BookingCancellation> findWithoutWorkflow(Set<CancellationStatus> statuses, int limit);

    boolean existsByRequesterAndStatusIn(UUID userId, java.util.Collection<CancellationStatus> statuses);
}
