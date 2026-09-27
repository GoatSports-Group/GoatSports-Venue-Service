package iuh.fit.venueservice.application.port;

import java.util.UUID;

public interface BookingNotificationPort {
    /**
     * Thông báo cho người trả tiền về một booking. Best-effort: lỗi gửi chỉ ghi log, không làm hỏng xử lý tiền.
     * {@code dedupeKey} cố định theo sự kiện nghiệp vụ nên Kafka xử lý lại không sinh thông báo trùng.
     */
    void notifyPayer(UUID receiverId, UUID bookingId, String dedupeKey, String title, String content);
}
