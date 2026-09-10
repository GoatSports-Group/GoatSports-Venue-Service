package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record OwnerBookingFilter(
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
    private static final int MAX_PAGE_SIZE = 20;
    private static final int MAX_DATE_RANGE_DAYS = 366;

    public void validate() throws ServiceException {
        if (ownerId == null) throw new ServiceException("Không xác định được chủ sân.");
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ServiceException("Trang phải từ 0 và kích thước trang từ 1 đến 20.");
        }
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw new ServiceException("Khoảng ngày booking không hợp lệ.");
            }
            if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_DATE_RANGE_DAYS) {
                throw new ServiceException("Khoảng ngày booking tối đa 366 ngày.");
            }
        }
        if (query != null && query.trim().length() > 100) {
            throw new ServiceException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
    }
}
