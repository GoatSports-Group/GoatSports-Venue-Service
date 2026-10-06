package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.stats.PlatformBookingStats;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.PlatformStatsRepository;
import iuh.fit.venueservice.application.service.AdminStatsService;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {
    /** Đơn đã giữ chỗ và đã trả tiền: tính vào giá trị giao dịch (GMV). */
    static final Set<BookingStatus> PAID = EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);
    private static final int MAX_RANGE_DAYS = 366;
    private static final int TOP_VENUES = 5;

    private final PlatformStatsRepository statsRepository;

    @Override
    public PlatformBookingStats getBookingStats(LocalDate fromDate, LocalDate toDate) throws ServiceException {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ServiceException("Khoảng ngày thống kê không hợp lệ.");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_RANGE_DAYS) {
            throw new ServiceException("Khoảng ngày thống kê tối đa 366 ngày.");
        }
        var from = fromDate.atStartOfDay();
        var to = toDate.plusDays(1).atStartOfDay();

        // ponytail: gom theo ngày trong bộ nhớ trên 3 trường mỗi đơn; đủ cho kỳ tối đa 1 năm ở quy mô hiện tại,
        // chuyển sang GROUP BY theo ngày trong SQL (hoặc bảng tổng hợp theo ngày) nếu số đơn lên hàng triệu.
        var rows = statsRepository.bookingRows(from, to);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) byStatus.put(status.name(), 0L);
        Map<LocalDate, Long> dayCounts = new TreeMap<>();
        Map<LocalDate, Double> dayGmv = new TreeMap<>();
        for (LocalDate day = fromDate; !day.isAfter(toDate); day = day.plusDays(1)) {
            dayCounts.put(day, 0L);
            dayGmv.put(day, 0D);
        }
        double gmv = 0;
        for (var row : rows) {
            byStatus.merge(row.status().name(), 1L, Long::sum);
            LocalDate day = row.createdAt().toLocalDate();
            dayCounts.merge(day, 1L, Long::sum);
            // Khong den nhan san: co so chi giu tien coc, khoan con lai khong thu.
            Double value = PAID.contains(row.status()) ? row.totalPrice()
                    : row.status() == BookingStatus.NO_SHOW ? row.depositAmount() : null;
            if (value != null) {
                gmv += value;
                dayGmv.merge(day, value, Double::sum);
            }
        }
        List<PlatformBookingStats.DayStat> byDay = new ArrayList<>();
        dayCounts.forEach((day, count) -> byDay.add(new PlatformBookingStats.DayStat(day, count, dayGmv.getOrDefault(day, 0D))));

        return new PlatformBookingStats(fromDate, toDate, statsRepository.countActiveVenues(),
                statsRepository.countActiveCourts(), rows.size(), gmv, byStatus, byDay,
                statsRepository.topVenues(from, to, PAID, TOP_VENUES));
    }
}
