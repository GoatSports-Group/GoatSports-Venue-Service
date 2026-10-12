package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Bộ lọc đơn đặt sân toàn nền tảng cho admin: như của chủ sân nhưng không giới hạn theo chủ sân. */
public record AdminBookingFilter(
        UUID ownerId,
        UUID venueId,
        UUID venueCourtId,
        BookingStatus status,
        String query,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
) {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DATE_RANGE_DAYS = 366;

    public void validate() throws ServiceException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ServiceException("Trang phải từ 0 và kích thước trang từ 1 đến 50.");
        }
        validateCriteria();
    }

    /** Cùng tiêu chí lọc, một trang chứa tối đa {@code limit} dòng (xuất báo cáo, không qua giới hạn trang của màn hình). */
    public AdminBookingFilter forExport(int limit) {
        return new AdminBookingFilter(ownerId, venueId, venueCourtId, status, query, fromDate, toDate, 0, limit);
    }

    /** Kiểm tra khoảng ngày và từ khóa (dùng cả cho xuất báo cáo). */
    public void validateCriteria() throws ServiceException {
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) throw new ServiceException("Khoảng ngày không hợp lệ.");
            if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_DATE_RANGE_DAYS) {
                throw new ServiceException("Khoảng ngày tối đa 366 ngày.");
            }
        }
        if (query != null && query.trim().length() > 100) {
            throw new ServiceException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
    }
}
