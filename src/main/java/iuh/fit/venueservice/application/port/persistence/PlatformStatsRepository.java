package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.response.stats.PlatformBookingStats;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Truy vấn tổng hợp cho dashboard admin; không thuộc về một aggregate nào. */
public interface PlatformStatsRepository {
    long countActiveVenues();

    long countActiveCourts();

    /** Đơn tạo trong [from, to): chỉ các trường cần để đếm theo trạng thái và gom theo ngày. */
    List<BookingRow> bookingRows(LocalDateTime from, LocalDateTime to);

    List<PlatformBookingStats.VenueStat> topVenues(LocalDateTime from, LocalDateTime to,
                                                   Collection<BookingStatus> paidStatuses, int limit);

    record BookingRow(LocalDateTime createdAt, BookingStatus status, Double totalPrice, Double depositAmount) { }
}
