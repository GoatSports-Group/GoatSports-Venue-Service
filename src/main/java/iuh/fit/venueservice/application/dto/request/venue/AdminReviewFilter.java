package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.ReviewStatus;

import java.util.UUID;

/** Bộ lọc đánh giá toàn nền tảng cho admin kiểm duyệt. Không bao giờ trả đánh giá người chơi đã tự xóa. */
public record AdminReviewFilter(UUID venueId, ReviewStatus status, Integer rating, int page, int size) {
    private static final int MAX_PAGE_SIZE = 50;

    public void validate() throws ServiceException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ServiceException("Trang phải từ 0 và kích thước trang từ 1 đến 50.");
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new ServiceException("Bộ lọc số sao phải từ 1 đến 5.");
        }
        if (status == ReviewStatus.DELETED) throw new ServiceException("Không xem được đánh giá đã xóa.");
    }
}
