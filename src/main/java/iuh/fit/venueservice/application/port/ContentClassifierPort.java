package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.domain.entity.AiVerdict;

import java.util.Optional;

/** Phân loại nội dung đánh giá bằng AI (ai-service). Chỉ gửi phần chữ, không gửi mã hay tên người chơi. */
public interface ContentClassifierPort {
    /**
     * Rỗng khi AI tạm thời không dùng được (chưa cấu hình, hết hạn mức, mạng lỗi): để nguyên, lần quét sau thử lại.
     *
     * @throws Unclassifiable khi AI trả lời nhưng không dùng được cho riêng đoạn chữ này
     */
    Optional<AiVerdict> classify(String kind, String text);

    class Unclassifiable extends RuntimeException {
        public Unclassifiable(String message) {
            super(message);
        }
    }
}
