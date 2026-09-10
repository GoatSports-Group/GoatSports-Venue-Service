package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record CreatePlayerReviewCommand(
        UUID playerId,
        UUID bookingId,
        Integer rating,
        String content
) {
    private static final int MAX_CONTENT_LENGTH = 2000;

    public void validate() throws ServiceException {
        if (playerId == null || bookingId == null || rating == null) {
            throw new ServiceException("Player, booking và số sao là bắt buộc.");
        }
        if (rating < 1 || rating > 5) {
            throw new ServiceException("Số sao phải từ 1 đến 5.");
        }
        if (content != null && content.trim().length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException("Nội dung đánh giá tối đa 2000 ký tự.");
        }
    }
}
