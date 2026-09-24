package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.MatchmakingLifecycleSyncPort;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.BookingCancellationService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationReviewMode;
import iuh.fit.venueservice.domain.enums.CancellationStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction do {@code BookingCancellationDecorator} (infrastructure) mở; lớp này không phụ thuộc Spring.
 * Mọi thao tác khóa booking/yêu cầu hủy (findByIdForUpdate) trước khi đổi trạng thái để hai yêu cầu song song
 * (người chơi hủy + chủ sân duyệt) không hoàn tiền hai lần.
 */
public class BookingCancellationServiceImpl implements BookingCancellationService {
    private final BookingRepository bookingRepository;
    private final BookingCancellationRepository cancellationRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final PaymentRefundPort paymentRefundPort;
    private final MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    private final Clock clock;
    private final Duration autoApprovalWindow;

    public BookingCancellationServiceImpl(
            BookingRepository bookingRepository,
            BookingCancellationRepository cancellationRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            PaymentRefundPort paymentRefundPort,
            MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort,
            Clock clock,
            Duration autoApprovalWindow
    ) {
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
        this.paymentRefundPort = paymentRefundPort;
        this.matchmakingLifecycleSyncPort = matchmakingLifecycleSyncPort;
        this.clock = clock;
        this.autoApprovalWindow = autoApprovalWindow;
    }

    @Override
    public BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException {
        command.validate();
        Booking booking = bookingRepository.findByIdForUpdate(command.bookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));

        if (!booking.getPlayerId().equals(command.requestedByUserId())) {
            throw new ServiceException("Bạn không có quyền yêu cầu hủy đơn đặt này.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ServiceException("Trạng thái đơn hiện tại không cho phép yêu cầu hủy.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime playStart = LocalDateTime.of(booking.getPlayDate(), booking.getStartTime());
        if (!playStart.isAfter(now)) {
            throw new ServiceException("Không thể hủy booking đã bắt đầu.");
        }
        if (cancellationRepository.findByBookingId(booking.getBookingId()).isPresent()) {
            throw new ServiceException("Booking này đã có yêu cầu hủy.");
        }
        Venue venue = venueOf(booking, "Không tìm thấy sân con của đơn đặt.", "Không tìm thấy cụm sân của đơn đặt.");

        // Chưa trả cọc thì hủy ngay, không có gì để hoàn. Vừa đặt xong (trong cửa sổ tự duyệt) thì hoàn đủ 100%
        // để người chơi bấm nhầm không bị mất tiền. Còn lại chủ sân duyệt theo chính sách hủy của cơ sở.
        boolean unpaidBooking = booking.getStatus() == BookingStatus.PENDING_PAYMENT && booking.getDepositPaymentId() == null;
        boolean automatic = unpaidBooking || isWithinAutoApprovalWindow(booking, now);
        double refundPercentage = unpaidBooking ? 0.0
                : automatic ? 100.0
                : venue.getCancellationPolicy() == null ? 0.0
                : venue.getCancellationPolicy().refundPercentage(Duration.between(now, playStart).toHours());
        double refundableDeposit = booking.getDepositPaymentId() == null || booking.getDepositAmount() == null
                ? 0.0 : booking.getDepositAmount();

        BookingCancellation saved = cancellationRepository.save(BookingCancellation.builder()
                .bookingId(booking.getBookingId())
                .requestedByUserId(command.requestedByUserId())
                .reason(command.reason().trim())
                .refundPercentage(refundPercentage)
                .refundAmount((double) Math.round(refundableDeposit * refundPercentage / 100.0))
                .status(CancellationStatus.PENDING)
                .reviewMode(automatic ? CancellationReviewMode.AUTOMATIC : CancellationReviewMode.OWNER_REVIEW)
                .build());
        if (!automatic) return saved;

        String automaticReason = unpaidBooking
                ? "Tự động hủy booking chưa thanh toán."
                : "Tự động chấp thuận trong vòng 60 phút kể từ lúc đặt.";
        return approve(saved, booking, command.requestedByUserId(), null, automaticReason);
    }

    @Override
    public BookingCancellation processCancellation(UUID cancellationId, boolean approved, UUID processedByUserId,
                                                   String processNote) throws ServiceException {
        BookingCancellation cancellation = cancellationRepository.findByIdForUpdate(cancellationId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy yêu cầu hủy sân."));

        // Bấm lại cùng một quyết định (mạng chập chờn, double-click) thì trả kết quả cũ thay vì báo lỗi.
        if (cancellation.getStatus() != CancellationStatus.PENDING) {
            if ((approved && cancellation.getStatus() != CancellationStatus.REJECTED)
                    || (!approved && cancellation.getStatus() == CancellationStatus.REJECTED)) {
                return cancellation;
            }
            throw new ServiceException("Yêu cầu hủy đã được xử lý với quyết định khác.");
        }

        Booking booking = bookingRepository.findByIdForUpdate(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));
        assertOwner(booking, processedByUserId, "Bạn không có quyền xử lý yêu cầu hủy của booking này.");

        if (!approved) {
            if (processNote == null || processNote.isBlank()) {
                throw new ServiceException("Chủ sân cần nhập lý do từ chối yêu cầu hủy.");
            }
            cancellation.setProcessedBy(processedByUserId);
            cancellation.setDecisionReason(processNote.trim());
            cancellation.setProcessedAt(LocalDateTime.now(clock));
            cancellation.setStatus(CancellationStatus.REJECTED);
            return cancellationRepository.save(cancellation);
        }
        return approve(cancellation, booking, processedByUserId, processedByUserId, processNote);
    }

    @Override
    public BookingCancellation completeManualRefund(UUID cancellationId, UUID ownerId, String providerRefundId)
            throws ServiceException {
        if (providerRefundId == null || providerRefundId.isBlank() || providerRefundId.trim().length() > 100) {
            throw new ServiceException("Mã giao dịch hoàn tiền là bắt buộc và không được vượt quá 100 ký tự.");
        }
        BookingCancellation cancellation = cancellationRepository.findByIdForUpdate(cancellationId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy yêu cầu hủy sân."));
        if (cancellation.getStatus() == CancellationStatus.REFUNDED) return cancellation;
        if (cancellation.getStatus() != CancellationStatus.REFUND_MANUAL_REVIEW || cancellation.getRefundId() == null) {
            throw new ServiceException("Yêu cầu hủy không ở trạng thái chờ xác nhận hoàn tiền thủ công.");
        }

        Booking booking = bookingRepository.findByIdForUpdate(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));
        assertOwner(booking, ownerId, "Bạn không có quyền xác nhận hoàn tiền cho booking này.");

        RefundSnapshot refund = paymentRefundPort.completeManualRefund(cancellation.getRefundId(), ownerId,
                providerRefundId.trim());
        if (!"SUCCEEDED".equals(refund.status())) {
            throw new ServiceException("Payment Service chưa xác nhận hoàn tiền thành công.");
        }
        cancellation.setStatus(CancellationStatus.REFUNDED);
        booking.setStatus(BookingStatus.REFUNDED);
        Booking saved = bookingRepository.save(booking);
        matchmakingLifecycleSyncPort.syncBookingLifecycle(
                saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name(), "REFUNDED");
        return cancellationRepository.save(cancellation);
    }

    /**
     * Chấp thuận hủy: có tiền hoàn thì gửi yêu cầu hoàn sang payment-service (idempotency key theo cancellationId,
     * gọi lại không hoàn hai lần), trả khung giờ cho người khác đặt và báo matchmaking nếu đơn đến từ ghép trận.
     */
    private BookingCancellation approve(BookingCancellation cancellation, Booking booking, UUID refundRequestedBy,
                                        UUID processedBy, String processNote) throws ServiceException {
        cancellation.setProcessedBy(processedBy);
        cancellation.setDecisionReason(processNote == null || processNote.isBlank() ? null : processNote.trim());
        cancellation.setProcessedAt(LocalDateTime.now(clock));
        if (cancellation.getRefundAmount() != null && cancellation.getRefundAmount() > 0) {
            if (booking.getDepositPaymentId() == null) {
                throw new ServiceException("Booking chưa có giao dịch tiền cọc để hoàn tiền.");
            }
            RefundSnapshot refund = paymentRefundPort.createRefund(
                    booking.getDepositPaymentId(),
                    refundRequestedBy,
                    "booking-cancellation:" + cancellation.getCancellationId(),
                    refundReason(booking, cancellation),
                    cancellation.getRefundAmount(),
                    processedBy != null
            );
            cancellation.setRefundId(refund.refundId());
            applyRefundStatus(refund.status(), cancellation, booking);
        } else {
            cancellation.setStatus(CancellationStatus.APPROVED);
            booking.setStatus(BookingStatus.CANCELLED);
        }
        timeSlotLifecycleService.releaseBookingSlot(booking);
        Booking saved = bookingRepository.save(booking);
        if (saved.getStatus() == BookingStatus.CANCELLED) {
            matchmakingLifecycleSyncPort.syncBookingLifecycle(
                    saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name(), "PLAYER_CANCELLED");
        } else if (saved.getStatus() == BookingStatus.REFUNDED) {
            matchmakingLifecycleSyncPort.syncBookingLifecycle(
                    saved.getMatchmakingSessionId(), saved.getBookingId(), saved.getStatus().name(), "REFUNDED");
        }
        return cancellationRepository.save(cancellation);
    }

    /** Ánh xạ trạng thái khoản hoàn từ payment-service sang trạng thái yêu cầu hủy và đơn đặt sân. */
    private void applyRefundStatus(String refundStatus, BookingCancellation cancellation, Booking booking) {
        if ("SUCCEEDED".equals(refundStatus)) {
            cancellation.setStatus(CancellationStatus.REFUNDED);
            booking.setStatus(BookingStatus.REFUNDED);
            return;
        }
        // Mọi trạng thái còn lại: tiền chưa về tài khoản người chơi, đơn ở REFUND_PENDING.
        booking.setStatus(BookingStatus.REFUND_PENDING);
        cancellation.setStatus(switch (refundStatus == null ? "" : refundStatus) {
            case "MANUAL_REVIEW" -> CancellationStatus.REFUND_MANUAL_REVIEW;
            case "FAILED" -> CancellationStatus.REFUND_FAILED;
            case "AWAITING_CLAIM" -> CancellationStatus.REFUND_AWAITING_CLAIM;
            case "AWAITING_BANK_ACCOUNT" -> CancellationStatus.REFUND_AWAITING_BANK_ACCOUNT;
            default -> CancellationStatus.REFUND_PROCESSING;
        });
    }

    private boolean isWithinAutoApprovalWindow(Booking booking, LocalDateTime now) {
        return booking.getCreatedAt() != null
                && !now.isBefore(booking.getCreatedAt())
                && !now.isAfter(booking.getCreatedAt().plus(autoApprovalWindow));
    }

    private void assertOwner(Booking booking, UUID userId, String message) throws ServiceException {
        Venue venue = venueOf(booking, "Không tìm thấy sân của booking.", "Không tìm thấy cơ sở của booking.");
        if (venue.getOwnerId() == null || !venue.getOwnerId().equals(userId)) throw new OwnershipException(message);
    }

    private Venue venueOf(Booking booking, String courtMissing, String venueMissing) throws ServiceException {
        VenueCourt court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException(courtMissing));
        return venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException(venueMissing));
    }

    /** payment-service giới hạn lý do hoàn 500 ký tự. */
    private String refundReason(Booking booking, BookingCancellation cancellation) {
        String reason = "Hủy booking " + booking.getBookingCode() + ": " + cancellation.getReason();
        return reason.length() <= 500 ? reason : reason.substring(0, 500);
    }
}
