package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.dto.request.payment.RefundResultCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.CancellationStatus;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public class BookingPaymentEventServiceImpl implements BookingPaymentEventService {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final String LATE_TITLE = "Hết giờ giữ sân — đang hoàn tiền";
    private static final String LATE_DETAIL = "về sau khi lượt đặt đã hết giờ giữ chỗ hoặc đã hủy nên không giữ được sân.";
    private final BookingRepository bookingRepository;
    private final BookingQrPort bookingQrPort;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    private final PaymentRefundPort paymentRefundPort;
    private final BookingNotificationPort bookingNotificationPort;

    public BookingPaymentEventServiceImpl(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            BookingCancellationRepository bookingCancellationRepository,
            MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort,
            PaymentRefundPort paymentRefundPort,
            BookingNotificationPort bookingNotificationPort
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingQrPort = bookingQrPort;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
        this.bookingCancellationRepository = bookingCancellationRepository;
        this.matchmakingLifecycleSyncPort = matchmakingLifecycleSyncPort;
        this.paymentRefundPort = paymentRefundPort;
        this.bookingNotificationPort = bookingNotificationPort;
    }

    @Override
    public boolean applyTerminalPayment(PaymentTerminalCommand command) throws ServiceException {
        if (command == null || !command.isBookingDepositTerminal()) {
            return false;
        }
        command.validateIdentity();

        Booking booking = bookingRepository.findByIdForUpdate(command.referenceId()).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return false;
        }
        if (!isExpectedPayer(booking, command.payerId())) {
            throw new ServiceException("Payer của payment terminal event không khớp booking.");
        }

        boolean paymentCancelled = "CANCELLED".equals(command.status());
        booking.setStatus(paymentCancelled ? BookingStatus.CANCELLED : BookingStatus.EXPIRED);
        timeSlotLifecycleService.releaseBookingSlot(booking);
        Booking saved = bookingRepository.save(booking);
        matchmakingLifecycleSyncPort.syncBookingLifecycle(
                saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name(),
                paymentCancelled ? "PAYMENT_CANCELLED" : "PAYMENT_EXPIRED"
        );
        return true;
    }

    @Override
    public boolean applyRefundResult(RefundResultCommand command) throws ServiceException {
        command.validateIdentity();
        var cancellation = bookingCancellationRepository.findByRefundIdForUpdate(command.refundId()).orElse(null);
        if (cancellation == null) return false;
        Booking booking = bookingRepository.findByIdForUpdate(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy booking của refund."));
        if (!command.paymentId().equals(booking.getDepositPaymentId())) {
            throw new ServiceException("Payment của refund event không khớp booking.");
        }
        CancellationStatus status = switch (command.status()) {
            case "SUCCEEDED" -> CancellationStatus.REFUNDED;
            case "FAILED" -> CancellationStatus.REFUND_FAILED;
            case "AWAITING_BANK_ACCOUNT" -> CancellationStatus.REFUND_AWAITING_BANK_ACCOUNT;
            case "AWAITING_CLAIM" -> CancellationStatus.REFUND_AWAITING_CLAIM;
            case "MANUAL_REVIEW" -> CancellationStatus.REFUND_MANUAL_REVIEW;
            default -> CancellationStatus.REFUND_PROCESSING;
        };
        cancellation.setStatus(status);
        cancellation.setRefundFailureReason(
                status == CancellationStatus.REFUND_FAILED
                        || status == CancellationStatus.REFUND_AWAITING_BANK_ACCOUNT
                        ? command.failureReason()
                        : null
        );
        booking.setStatus(status == CancellationStatus.REFUNDED
                ? BookingStatus.REFUNDED : BookingStatus.REFUND_PENDING);
        bookingCancellationRepository.save(cancellation);
        Booking saved = bookingRepository.save(booking);
        if (status == CancellationStatus.REFUNDED) {
            matchmakingLifecycleSyncPort.syncBookingLifecycle(
                    saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name(), "REFUNDED"
            );
        }
        return true;
    }

    @Override
    public boolean applySucceededPayment(PaymentSucceededCommand command) throws ServiceException {
        if (command == null || !command.isBookingPaymentSucceeded()) {
            return false;
        }
        command.validateIdentity();
        Booking booking = bookingRepository.findByIdForUpdate(command.referenceId()).orElse(null);
        if (booking == null) return false;
        if (!isExpectedPayer(booking, command.payerId())) {
            throw new ServiceException("Payer của payment event không khớp booking.");
        }

        if ("BOOKING_DEPOSIT".equals(command.purpose())) {
            ensureAmountMatches(command.amount(), booking.getDepositAmount());
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    && booking.getStatus() != BookingStatus.CONFIRMED) {
                // Sự kiện lặp của khoản cọc booking đã ghi nhận (rồi mới hủy): hủy sân tự lo phần hoàn theo chính sách.
                if (command.paymentId().equals(booking.getDepositPaymentId())) return false;
                // Tiền cọc về sau khi booking đã hết hạn giữ chỗ / đã hủy: sân không còn giữ nên hoàn đủ.
                return refundUnaccepted(booking, command,
                        "Tiền cọc về sau khi lượt đặt sân đã hết hạn giữ chỗ hoặc đã hủy — hoàn lại toàn bộ",
                        LATE_TITLE, LATE_DETAIL);
            }
            if (booking.getDepositPaymentId() != null && !booking.getDepositPaymentId().equals(command.paymentId())) {
                return refundUnaccepted(booking, command,
                        "Lượt đặt sân đã được đóng cọc bằng giao dịch khác — hoàn khoản trùng",
                        "Khoản thanh toán trùng — đang hoàn tiền",
                        "là khoản trùng: lượt đặt đã được đóng cọc bằng giao dịch khác, sân của bạn vẫn giữ nguyên.");
            }
            // Gọi trước khi commit: lỗi thì cả sự kiện được Kafka xử lý lại; gọi lặp thì payment-service bỏ qua.
            UUID ownerId = venueOwnerId(booking);
            paymentRefundPort.assignPayee(command.paymentId(), ownerId);
            boolean changed = !command.paymentId().equals(booking.getDepositPaymentId());
            booking.setDepositPaymentId(command.paymentId());
            boolean confirmedNow = booking.getStatus() == BookingStatus.PENDING_PAYMENT;
            if (confirmedNow) {
                booking.setStatus(BookingStatus.CONFIRMED);
                changed = true;
            }
            if (booking.getQrCode() == null || booking.getQrCode().isBlank()) {
                booking.setQrCode(bookingQrPort.issue(
                        booking.getBookingId(), LocalDateTime.of(booking.getPlayDate(), booking.getEndTime())
                ));
                changed = true;
            }
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                    || booking.getStatus() == BookingStatus.CONFIRMED) {
                timeSlotLifecycleService.confirmBookingSlot(booking);
            }
            if (changed) {
                Booking saved = bookingRepository.save(booking);
                matchmakingLifecycleSyncPort.syncBookingLifecycle(
                        saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name()
                );
            }
            // Don moi giu cho chac chan: bao chu san (dashboard lam moi tinh trang san qua WebSocket thong bao).
            if (confirmedNow) {
                bookingNotificationPort.notifyOwner(ownerId, booking.getBookingId(),
                        "booking-confirmed-owner:" + booking.getBookingId(), "Đơn đặt sân mới",
                        "Lượt " + booking.getBookingCode() + " " + slotLabel(booking) + " đã đóng cọc.");
            }
            return changed;
        }

        if ("BOOKING_REMAINING".equals(command.purpose())) {
            ensureAmountMatches(command.amount(), booking.getRemainingAmount());
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    && booking.getStatus() != BookingStatus.CONFIRMED
                    && booking.getStatus() != BookingStatus.CHECKED_IN
                    && booking.getStatus() != BookingStatus.COMPLETED) {
                // Tiền còn lại về cho booking đã hết hạn / đã hủy: không có gì để thu nữa, hoàn đủ.
                if (booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.CANCELLED) {
                    return refundUnaccepted(booking, command,
                            "Tiền còn lại về sau khi lượt đặt sân đã hết hạn hoặc đã hủy — hoàn lại toàn bộ",
                            LATE_TITLE, LATE_DETAIL);
                }
                return false;
            }
            paymentRefundPort.assignPayee(command.paymentId(), venueOwnerId(booking));
            boolean changed = !command.paymentId().equals(booking.getRemainingPaymentId());
            booking.setRemainingPaymentId(command.paymentId());
            if (booking.getSource() == BookingSource.WALK_IN
                    && booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
                booking.setStatus(BookingStatus.CONFIRMED);
                changed = true;
            }
            if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                    || booking.getStatus() == BookingStatus.CONFIRMED) {
                timeSlotLifecycleService.confirmBookingSlot(booking);
            }
            if (changed) {
                Booking saved = bookingRepository.save(booking);
                matchmakingLifecycleSyncPort.syncBookingLifecycle(
                        saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name()
                );
            }
            return changed;
        }
        return false;
    }

    /**
     * Tiền đã vào tài khoản nhưng booking không nhận được nữa: tạo lệnh hoàn toàn bộ, tự chuyển về ngân hàng
     * (chưa liên kết thì chờ, liên kết xong tự nhận). Khóa idempotency theo paymentId nên sự kiện lặp không hoàn hai lần.
     * Gọi payment-service lỗi thì ném ra để Kafka thử lại (hết lượt thì vào topic -dlt), không nuốt mất tiền.
     * Khoản hoàn được ghi lên booking để màn hình kết quả / chi tiết đặt sân nói đúng sự thật thay vì
     * "thanh toán thành công", và người trả nhận một thông báo riêng (khóa dedupe theo paymentId).
     */
    private boolean refundUnaccepted(Booking booking, PaymentSucceededCommand command, String reason,
                                     String title, String detail) {
        paymentRefundPort.createRefund(command.paymentId(), command.payerId(), "unaccepted-payment:" + command.paymentId(),
                reason, command.amount(), true);
        if (!command.paymentId().equals(booking.getLateRefundPaymentId())) {
            booking.setLateRefundPaymentId(command.paymentId());
            booking.setLateRefundAmount(command.amount());
            booking.setLateRefundAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
        bookingNotificationPort.notifyPayer(command.payerId(), booking.getBookingId(),
                "late-payment-refund:" + command.paymentId(), title,
                "Khoản " + NumberFormat.getIntegerInstance(VI).format(command.amount()) + " đ cho lượt đặt "
                        + booking.getBookingCode() + " " + detail
                        + " GOAT SPORTS đang hoàn toàn bộ: tiền tự về tài khoản ngân hàng đã liên kết,"
                        + " chưa liên kết thì vào Cài đặt › Ngân hàng & hoàn tiền để nhận.");
        return true;
    }

    private boolean isExpectedPayer(Booking booking, UUID payerId) throws ServiceException {
        if (booking.getSource() != BookingSource.WALK_IN) {
            return booking.getPlayerId() != null && booking.getPlayerId().equals(payerId);
        }
        return payerId.equals(venueOwnerId(booking));
    }

    /** "lúc 19:00 ngày 11/10/2026" cho nội dung thông báo. */
    static String slotLabel(Booking booking) {
        return "lúc " + booking.getStartTime().toString().substring(0, 5) + " ngày "
                + booking.getPlayDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private UUID venueOwnerId(Booking booking) throws ServiceException {
        var court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân của booking."));
        var venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở của booking."));
        return venue.getOwnerId();
    }

    private void ensureAmountMatches(Double actual, Double expected) throws ServiceException {
        if (actual == null || expected == null || Math.abs(actual - expected) >= 0.01) {
            throw new ServiceException("Số tiền payment event không khớp booking.");
        }
    }
}
