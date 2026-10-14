package iuh.fit.venueservice.domain.entity;

import java.time.LocalDateTime;

/**
 * Gợi ý kiểm duyệt của AI cho một đánh giá: nhãn (NONE, SPAM, HARASSMENT, HATE, SEXUAL, VIOLENCE, SCAM, OTHER),
 * mức 0–3 và một câu lý do. Chỉ admin thấy; AI không bao giờ tự ẩn hay gỡ đánh giá.
 */
public record AiVerdict(String label, Integer severity, String reason, LocalDateTime checkedAt) {
    /** Mức từ đây trở lên là "AI nghi vi phạm" trên trang Đánh giá của admin. */
    public static final int FLAG_SEVERITY = 2;
}
