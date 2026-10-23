package iuh.fit.venueservice.application.port;

import java.util.UUID;

public interface ExternalVenueNotificationPort {
    /** Báo người chơi kết quả duyệt sân họ gợi ý. Best-effort: lỗi gửi chỉ ghi log. */
    void notifySuggester(UUID receiverId, UUID suggestionId, String title, String content);
}
