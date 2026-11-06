package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Process "Vòng đời đặt sân" ở workflow-service (Camunda). Thao tác của người dùng (đóng cọc, thu tiền, nhận sân,
 * hủy) venue-service vẫn ghi ngay rồi báo process đi tiếp; các mốc giờ (hết giữ chỗ, không đến, hoàn thành) do process
 * gọi lại venue-service. Không gọi được workflow-service thì đơn vẫn chạy: job nhận nuôi mở process sau, các timer của
 * process tự đối soát nếu lỡ message.
 */
public interface BookingLifecyclePort {
    ZoneId VENUE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Mở process ngay trong transaction đang ghi đơn.
     *
     * @return process instance key, hoặc null khi workflow-service chưa sẵn sàng
     */
    Long start(StartBookingLifecycle command);

    /** Gửi sau khi transaction commit; lỗi chỉ ghi log. Gửi lặp cùng {@code messageId} thì process bỏ qua. */
    void publish(UUID bookingId, LifecycleMessage message, String messageId);

    enum LifecycleMessage {
        DEPOSIT_PAID("BookingDepositPaid"),
        PAYMENT_CLOSED("BookingPaymentClosed"),
        REMAINING_PAID("BookingRemainingPaid"),
        CHECKED_IN("BookingCheckedIn"),
        CANCELLED("BookingCancelled");

        private final String messageName;

        LifecycleMessage(String messageName) {
            this.messageName = messageName;
        }

        public String messageName() {
            return messageName;
        }
    }

    /**
     * @param holdExpiresAt ISO-8601 có offset; null khi đơn không chờ cọc
     * @param playEndAt     ISO-8601 có offset
     */
    record StartBookingLifecycle(UUID bookingId, String status, boolean remainingPaid, String holdExpiresAt,
                                 String playEndAt) {
        public static StartBookingLifecycle of(Booking booking) {
            return new StartBookingLifecycle(
                    booking.getBookingId(),
                    booking.getStatus().name(),
                    booking.getRemainingPaymentId() != null
                            || booking.getRemainingAmount() == null || booking.getRemainingAmount() <= 0,
                    booking.getStatus() == BookingStatus.PENDING_PAYMENT ? iso(holdDeadline(booking)) : null,
                    iso(LocalDateTime.of(booking.getPlayDate(), booking.getEndTime()))
            );
        }

        /** Đơn chờ cọc mà không có hạn giữ chỗ (dữ liệu cũ) thì giữ tới giờ bắt đầu. */
        private static LocalDateTime holdDeadline(Booking booking) {
            return booking.getHoldExpiresAt() != null
                    ? booking.getHoldExpiresAt()
                    : LocalDateTime.of(booking.getPlayDate(), booking.getStartTime());
        }

        private static String iso(LocalDateTime value) {
            return value.atZone(VENUE_ZONE).toOffsetDateTime().toString();
        }
    }
}
