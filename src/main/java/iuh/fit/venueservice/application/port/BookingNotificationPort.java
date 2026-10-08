package iuh.fit.venueservice.application.port;

import java.util.UUID;

public interface BookingNotificationPort {
    /**
     * Thông báo cho người trả tiền về một booking. Best-effort: lỗi gửi chỉ ghi log, không làm hỏng xử lý tiền.
     * {@code dedupeKey} cố định theo sự kiện nghiệp vụ nên Kafka xử lý lại không sinh thông báo trùng.
     */
    void notifyPayer(UUID receiverId, UUID bookingId, String dedupeKey, String title, String content);

    /**
     * Thông báo cho chủ sân về một lượt đặt tại cơ sở của họ (đơn mới đã cọc, khách hủy / xin hủy). Đi qua WebSocket
     * thông báo nên dashboard chủ sân làm mới tình trạng sân ngay. Best-effort như {@link #notifyPayer}.
     */
    void notifyOwner(UUID ownerId, UUID bookingId, String dedupeKey, String title, String content);
}
