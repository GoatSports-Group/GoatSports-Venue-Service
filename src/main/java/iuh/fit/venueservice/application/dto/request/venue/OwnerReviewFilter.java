package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record OwnerReviewFilter(
        UUID ownerId,
        UUID venueId,
        UUID venueCourtId,
        Integer rating,
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
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new ServiceException("Bộ lọc số sao phải từ 1 đến 5.");
        }
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw new ServiceException("Khoảng ngày đánh giá không hợp lệ.");
            }
            if (ChronoUnit.DAYS.between(fromDate, toDate) + 1 > MAX_DATE_RANGE_DAYS) {
                throw new ServiceException("Khoảng ngày đánh giá tối đa 366 ngày.");
            }
        }
    }
}
