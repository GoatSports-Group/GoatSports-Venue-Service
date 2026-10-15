package iuh.fit.venueservice.application.port;

import java.util.Map;
import java.util.UUID;

/**
 * Phát hành vi của người dùng (xem sân, đặt sân) sang ai-service để gợi ý cá nhân hóa, qua topic
 * {@code user-activity-topic}. Best-effort: lỗi phát không được làm hỏng nghiệp vụ gốc. Trong transaction thì chỉ
 * phát sau khi commit.
 */
public interface ActivityEventPublisher {
    String VENUE_VIEWED = "activity.venue.viewed.v1";
    String BOOKING_STATE = "activity.booking.state.v1";

    void publish(String eventType, UUID userId, Map<String, Object> payload);
}
