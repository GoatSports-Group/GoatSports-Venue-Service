package iuh.fit.venueservice.application.dto.response.stats;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Số liệu đặt sân toàn nền tảng trong một kỳ (theo ngày tạo đơn), cho dashboard admin.
 * {@code gmv}: tổng giá trị các đơn đã giữ chỗ và đã trả tiền (đã xác nhận, đã nhận sân, hoàn tất).
 */
public record PlatformBookingStats(
        LocalDate fromDate,
        LocalDate toDate,
        long activeVenues,
        long activeCourts,
        long bookings,
        double gmv,
        Map<String, Long> bookingsByStatus,
        List<DayStat> byDay,
        List<VenueStat> topVenues
) {
    public record DayStat(LocalDate date, long bookings, double gmv) { }

    public record VenueStat(UUID venueId, String name, long bookings, double gmv) { }
}
