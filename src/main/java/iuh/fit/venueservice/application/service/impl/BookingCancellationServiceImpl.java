package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.WorkflowCancellationStateCommand;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.CancellationWorkflowPort;
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
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Transaction do {@code BookingCancellationDecorator} (infrastructure) mở; lớp này không phụ thuộc Spring.
 *
 * <p>Trạng thái của yêu cầu hủy do process Camunda quyết định (workflow-service). Lớp này chỉ ghi trạng thái trong
 * {@link #applyWorkflowState}; các thao tác khác (người chơi hủy, chủ sân duyệt, xác nhận hoàn tay) chỉ ghi dữ liệu
 * nghiệp vụ hoặc chuyển quyết định vào process. Mọi bước khóa booking trước (findByIdForUpdate) nên lệnh của process
 * tới khi yêu cầu vừa tạo chưa commit sẽ đợi commit xong rồi mới đọc.
 */
public class BookingCancellationServiceImpl implements BookingCancellationService {
    private static final ZoneId VENUE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Trạng thái còn dở: phải có process Camunda đi kèm. */
    private static final Set<CancellationStatus> IN_PROGRESS = EnumSet.of(
            CancellationStatus.PENDING,
            CancellationStatus.REFUND_AWAITING_CLAIM,
            CancellationStatus.REFUND_AWAITING_BANK_ACCOUNT,
            CancellationStatus.REFUND_PROCESSING,
            CancellationStatus.REFUND_MANUAL_REVIEW,
            CancellationStatus.REFUND_FAILED
    );
    /** Đơn còn có thể đổi theo yêu cầu hủy (đơn đã hết hạn/đã xong do việc khác thì giữ nguyên). */
    private static final Set<BookingStatus> CANCELLABLE_BOOKING = EnumSet.of(
            BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT, BookingStatus.REFUND_PENDING);

    private final BookingRepository bookingRepository;
    private final BookingCancellationRepository cancellationRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final PaymentRefundPort paymentRefundPort;
    private final MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort;
    private final BookingNotificationPort bookingNotificationPort;
    private final CancellationWorkflowPort cancellationWorkflow;
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
            BookingNotificationPort bookingNotificationPort,
            CancellationWorkflowPort cancellationWorkflow,
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
        this.bookingNotificationPort = bookingNotificationPort;
        this.cancellationWorkflow = cancellationWorkflow;
        this.clock = clock;
        this.autoApprovalWindow = autoApprovalWindow;
    }

    /**
     * Ghi yêu cầu (PENDING) rồi mở process trong cùng transaction: workflow-service không mở được process thì cả yêu
     * cầu bị hủy bỏ, không bao giờ có yêu cầu hủy nằm trong DB mà Camunda không biết.
     */
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
        String automaticReason = !automatic ? null
                : unpaidBooking ? "Tự động hủy booking chưa thanh toán."
                : "Tự động chấp thuận trong vòng 60 phút kể từ lúc đặt.";
        saved.setWorkflowInstanceKey(
            cancellationWorkflow.start(
                new CancellationWorkflowPort.StartCancellationWorkflow(
                    saved.getCancellationId(), booking.getBookingId(), saved.getReviewMode().name(),
                    saved.getRefundAmount(), playStart.atZone(VENUE_ZONE).toOffsetDateTime().toString(), automaticReason,
                    null, null
                )
            )
        );
        saved = cancellationRepository.save(saved);

        String slot = BookingPaymentEventServiceImpl.slotLabel(booking);
        if (automatic) {
            bookingNotificationPort.notifyOwner(venue.getOwnerId(), booking.getBookingId(),
                    "cancellation-auto-owner:" + saved.getCancellationId(), "Khách đã hủy lượt đặt",
                    "Lượt " + booking.getBookingCode() + " " + slot + " đã được hủy, sân được trả lại.");
        } else {
            bookingNotificationPort.notifyOwner(venue.getOwnerId(), booking.getBookingId(),
                    "cancellation-requested-owner:" + saved.getCancellationId(), "Yêu cầu hủy cần bạn duyệt",
                    "Khách xin hủy lượt " + booking.getBookingCode() + " " + slot + ". Lý do: " + command.reason().trim());
        }
        return saved;
    }

    @Override
    public BookingCancellation processCancellation(UUID cancellationId, boolean approved, UUID processedByUserId,
                                                   String processNote) throws ServiceException {
        BookingCancellation cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy yêu cầu hủy sân."));
        Booking booking = bookingRepository.findById(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));
        assertOwner(booking, processedByUserId, "Bạn không có quyền xử lý yêu cầu hủy của booking này.");

        // Bấm lại cùng một quyết định (mạng chập chờn, double-click) thì trả kết quả cũ thay vì báo lỗi.
        if (cancellation.getStatus() != CancellationStatus.PENDING) {
            if ((approved && cancellation.getStatus() != CancellationStatus.REJECTED)
                    || (!approved && cancellation.getStatus() == CancellationStatus.REJECTED)) {
                return cancellation;
            }
            throw new ServiceException("Yêu cầu hủy đã được xử lý với quyết định khác.");
        }
        if (!approved && (processNote == null || processNote.isBlank())) {
            throw new ServiceException("Chủ sân cần nhập lý do từ chối yêu cầu hủy.");
        }
        String note = processNote == null || processNote.isBlank() ? null : processNote.trim();
        if (!cancellationWorkflow.decide(cancellationId, approved, processedByUserId, note)) {
            throw new ServiceException("Yêu cầu hủy đã được xử lý (hoặc đã tự chấp thuận khi tới giờ chơi). "
                    + "Vui lòng tải lại để xem trạng thái mới.");
        }
        return cancellation;
    }

    /** payment-service ghi khoản hoàn thành công và phát event; process nhận event đó rồi mới ghi REFUNDED. */
    @Override
    public BookingCancellation completeManualRefund(UUID cancellationId, UUID ownerId, String providerRefundId)
            throws ServiceException {
        if (providerRefundId == null || providerRefundId.isBlank() || providerRefundId.trim().length() > 100) {
            throw new ServiceException("Mã giao dịch hoàn tiền là bắt buộc và không được vượt quá 100 ký tự.");
        }
        BookingCancellation cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy yêu cầu hủy sân."));
        if (cancellation.getStatus() == CancellationStatus.REFUNDED) return cancellation;
        if (cancellation.getStatus() != CancellationStatus.REFUND_MANUAL_REVIEW || cancellation.getRefundId() == null) {
            throw new ServiceException("Yêu cầu hủy không ở trạng thái chờ xác nhận hoàn tiền thủ công.");
        }
        Booking booking = bookingRepository.findById(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));
        assertOwner(booking, ownerId, "Bạn không có quyền xác nhận hoàn tiền cho booking này.");

        RefundSnapshot refund = paymentRefundPort.completeManualRefund(cancellation.getRefundId(), ownerId,
                providerRefundId.trim());
        if (!"SUCCEEDED".equals(refund.status())) {
            throw new ServiceException("Payment Service chưa xác nhận hoàn tiền thành công.");
        }
        return cancellation;
    }

    /**
     * Bản chiếu trạng thái Camunda. Việc đi kèm (trả khung giờ, đổi trạng thái đơn, báo ghép trận) chỉ chạy khi
     * trạng thái thật sự đổi, nên process gửi lại cùng lệnh (thử lại, đối soát) không trả sân hai lần.
     */
    @Override
    public BookingCancellation applyWorkflowState(WorkflowCancellationStateCommand command) throws ServiceException {
        if (command.status() == null) throw new ServiceException("Thiếu trạng thái yêu cầu hủy.");
        Booking booking = bookingRepository.findByIdForUpdate(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        BookingCancellation cancellation = lockedCancellation(command.cancellationId(), booking);
        if (cancellation.getWorkflowVersion() != null && command.version() < cancellation.getWorkflowVersion()) {
            return cancellation;
        }
        cancellation.setWorkflowVersion(command.version());
        CancellationStatus previous = cancellation.getStatus();
        if (previous == command.status()) return cancellationRepository.save(cancellation);

        if (previous == CancellationStatus.PENDING) {
            cancellation.setProcessedBy(command.processedBy());
            cancellation.setDecisionReason(command.decisionNote() == null || command.decisionNote().isBlank()
                    ? null : command.decisionNote().trim());
            cancellation.setProcessedAt(LocalDateTime.now(clock));
        }
        cancellation.setStatus(command.status());
        cancellation.setRefundFailureReason(command.status() == CancellationStatus.REFUND_FAILED
                || command.status() == CancellationStatus.REFUND_AWAITING_BANK_ACCOUNT
                ? command.refundFailureReason() : null);

        BookingStatus target = switch (command.status()) {
            case APPROVED -> BookingStatus.CANCELLED;
            case REFUNDED -> BookingStatus.REFUNDED;
            case REFUND_AWAITING_CLAIM, REFUND_AWAITING_BANK_ACCOUNT, REFUND_PROCESSING, REFUND_MANUAL_REVIEW,
                 REFUND_FAILED -> BookingStatus.REFUND_PENDING;
            case PENDING, REJECTED -> null;
        };
        if (target != null && CANCELLABLE_BOOKING.contains(booking.getStatus()) && booking.getStatus() != target) {
            // Trả khung giờ đúng một lần: lúc yêu cầu rời PENDING sang nhánh chấp thuận. Gọi lại sau đó có thể nhả
            // nhầm khung giờ người khác vừa đặt.
            if (previous == CancellationStatus.PENDING && booking.getStatus() != BookingStatus.REFUND_PENDING) {
                timeSlotLifecycleService.releaseBookingSlot(booking);
            }
            booking.setStatus(target);
            Booking saved = bookingRepository.save(booking);
            if (target == BookingStatus.CANCELLED || target == BookingStatus.REFUNDED) {
                matchmakingLifecycleSyncPort.syncBookingLifecycle(saved.getMatchmakingSessionId(), saved.getBookingId(),
                        saved.getStatus().name(), target == BookingStatus.CANCELLED ? "PLAYER_CANCELLED" : "REFUNDED");
            }
        }
        return cancellationRepository.save(cancellation);
    }

    /** Tạo khoản hoàn (idempotency key theo yêu cầu hủy: process thử lại không hoàn hai lần). */
    @Override
    public RefundSnapshot createWorkflowRefund(UUID cancellationId, UUID bookingId, UUID processedBy)
            throws ServiceException {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        BookingCancellation cancellation = lockedCancellation(cancellationId, booking);
        if (booking.getDepositPaymentId() == null) {
            throw new ServiceException("Booking chưa có giao dịch tiền cọc để hoàn tiền.");
        }
        // Chủ sân duyệt thì payment-service tự chi về ngân hàng người chơi; tự duyệt thì người chơi bấm nhận.
        RefundSnapshot refund = paymentRefundPort.createRefund(
                booking.getDepositPaymentId(),
                processedBy != null ? processedBy : cancellation.getRequestedByUserId(),
                "booking-cancellation:" + cancellation.getCancellationId(),
                refundReason(booking, cancellation),
                cancellation.getRefundAmount(),
                processedBy != null
        );
        cancellation.setRefundId(refund.refundId());
        cancellationRepository.save(cancellation);
        return refund;
    }

    @Override
    public void remindOwnerToDecide(UUID cancellationId, UUID bookingId) throws ServiceException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        BookingCancellation cancellation = cancellationRepository.findById(cancellationId)
                .filter(item -> item.getBookingId().equals(bookingId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hủy sân."));
        if (cancellation.getStatus() != CancellationStatus.PENDING) return;
        Venue venue = venueOf(booking, "Không tìm thấy sân của booking.", "Không tìm thấy cơ sở của booking.");
        bookingNotificationPort.notifyOwner(venue.getOwnerId(), booking.getBookingId(),
                "cancellation-reminder-owner:" + cancellationId, "Yêu cầu hủy vẫn chờ bạn duyệt",
                "Khách xin hủy lượt " + booking.getBookingCode() + " " + BookingPaymentEventServiceImpl.slotLabel(booking)
                        + ". Nếu bạn chưa duyệt, hệ thống sẽ tự chấp thuận khi tới giờ chơi.");
    }

    @Override
    public List<UUID> findCancellationsWithoutWorkflow(int limit) {
        return cancellationRepository.findWithoutWorkflow(IN_PROGRESS, limit).stream()
                .map(BookingCancellation::getCancellationId)
                .toList();
    }

    @Override
    public void adoptIntoWorkflow(UUID cancellationId) throws ServiceException {
        BookingCancellation cancellation = cancellationRepository.findByIdForUpdate(cancellationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hủy sân."));
        if (cancellation.getWorkflowInstanceKey() != null || !IN_PROGRESS.contains(cancellation.getStatus())) return;
        Booking booking = bookingRepository.findById(cancellation.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        LocalDateTime playStart = LocalDateTime.of(booking.getPlayDate(), booking.getStartTime());
        cancellation.setWorkflowInstanceKey(cancellationWorkflow.start(new CancellationWorkflowPort.StartCancellationWorkflow(
                cancellationId, booking.getBookingId(), cancellation.getReviewMode().name(),
                cancellation.getRefundAmount() == null ? 0.0 : cancellation.getRefundAmount(),
                playStart.atZone(VENUE_ZONE).toOffsetDateTime().toString(), cancellation.getDecisionReason(),
                cancellation.getStatus().name(), cancellation.getRefundId())));
        cancellationRepository.save(cancellation);
    }

    private BookingCancellation lockedCancellation(UUID cancellationId, Booking booking) throws ServiceException {
        return cancellationRepository.findByIdForUpdate(cancellationId)
                .filter(item -> item.getBookingId().equals(booking.getBookingId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hủy sân."));
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
