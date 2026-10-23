package iuh.fit.venueservice.application.port;

import java.util.UUID;

public interface ExternalVenueNotificationPort {
    /** Báo người chơi kết quả duyệt sân họ gợi ý. Best-effort: lỗi gửi chỉ ghi log. */
    void notifySuggester(UUID receiverId, UUID suggestionId, String title, String content);

    /**
     * Báo người chơi đã bấm "muốn đặt online" rằng sân đã có chủ trên GOAT Sports; bấm mở cơ sở mới. Best-effort: false
     * khi gửi lỗi (gửi lại an toàn vì event id cố định theo sân và người nhận).
     */
    boolean notifyVenueJoined(UUID receiverId, UUID externalVenueId, UUID venueId, String title, String content);
}
