package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record PublicReviewFilter(UUID venueId, int page, int size) {
    public void validate() throws ServiceException {
        if (venueId == null) {
            throw new ServiceException("Không xác định được cơ sở cần xem đánh giá.");
        }
        if (page < 0 || size < 1 || size > 50) {
            throw new ServiceException("Thông tin phân trang đánh giá không hợp lệ.");
        }
    }
}
