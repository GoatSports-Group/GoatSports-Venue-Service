package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.PlayerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.PlayerReviewView;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.PaymentRefundPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationStatus;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BookingServiceImpl implements BookingService {
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_PLAYER_PAGE_SIZE = 50;

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final ReviewRepository reviewRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final BookingQrPort bookingQrPort;
    private final PaymentRefundPort paymentRefundPort;
    private final Clock clock;
    private final Duration paymentHoldDuration;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            BookingCancellationRepository bookingCancellationRepository,
            ReviewRepository reviewRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            BookingQrPort bookingQrPort,
            PaymentRefundPort paymentRefundPort,
            Clock clock,
            Duration paymentHoldDuration
    ) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.bookingCancellationRepository = bookingCancellationRepository;
        this.reviewRepository = reviewRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
        this.bookingQrPort = bookingQrPort;
        this.paymentRefundPort = paymentRefundPort;
        this.clock = clock;
        this.paymentHoldDuration = paymentHoldDuration;
    }

    @Override
    public List<TimeSlotResponseCommand> getAvailableSlots(UUID venueCourtId, LocalDate date) throws ServiceException {
        venueCourtRepository.findById(venueCourtId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con với ID: " + venueCourtId));

        return timeSlotRepository.findAvailableSlots(venueCourtId, date).stream()
                .map(slot -> new TimeSlotResponseCommand(
                        slot.getTimeSlotId(), slot.getVenueCourtId(), slot.getDate(),
                        slot.getStartTime(), slot.getEndTime(), slot.getPricePerHour(), slot.getStatus().name()
                ))
                .toList();
    }

    @Override
    public Booking createBooking(CreateBookingCommand command) throws ServiceException {
        VenueCourt court = venueCourtRepository.findById(command.venueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con."));

        if (!Boolean.TRUE.equals(court.getActive()) || court.getDeletedAt() != null) {
            throw new ServiceException("Sân đang tạm ngừng hoạt động.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime requestedStart = LocalDateTime.of(command.playDate(), command.startTime());
        if (!requestedStart.isAfter(now)) {
            throw new ServiceException("Không thể đặt khung giờ đã bắt đầu hoặc nằm trong quá khứ.");
        }

        double durationHours = Duration.between(command.startTime(), command.endTime()).toMinutes() / 60.0;
        if (durationHours <= 0) {
            throw new ServiceException("Khung giờ đặt không hợp lệ.");
        }

        TimeSlot slot = timeSlotRepository.findByCourtAndTimeForUpdate(
                        command.venueCourtId(), command.playDate(), command.startTime(), command.endTime()
                )
                .orElseThrow(() -> new ServiceException("Khung giờ chưa được cấu hình."));

        if (slot.getStatus() != iuh.fit.venueservice.domain.enums.TimeSlotStatus.AVAILABLE) {
            throw new ServiceException("Khung giờ không còn khả dụng.");
        }

        if (slot.getPricePerHour() == null) {
            throw new ServiceException("Khung giờ chưa có giá hợp lệ.");
        }

        double totalPrice = durationHours * slot.getPricePerHour();
        BookingSource source = command.source() == null ? BookingSource.DIRECT : command.source();
        boolean isWalkIn = source == BookingSource.WALK_IN;
        double depositAmount = isWalkIn ? 0.0 : totalPrice * 0.3;

        Booking booking = Booking.builder()
                .playerId(command.playerId())
                .venueCourtId(command.venueCourtId())
                .playDate(command.playDate())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .status(isWalkIn ? BookingStatus.CONFIRMED : BookingStatus.PENDING_PAYMENT)
                .source(source)
                .totalPrice(totalPrice)
                .depositAmount(depositAmount)
                .remainingAmount(totalPrice - depositAmount)
                .bookingCode(generateBookingCode())
                .walkInCustomerName(command.walkInCustomerName())
                .walkInCustomerPhone(command.walkInCustomerPhone())
                .matchmakingSessionId(command.matchmakingSessionId())
                .holdExpiresAt(isWalkIn ? null : now.plus(paymentHoldDuration))
                .build();

        slot.setStatus(isWalkIn
                ? iuh.fit.venueservice.domain.enums.TimeSlotStatus.BOOKED
                : iuh.fit.venueservice.domain.enums.TimeSlotStatus.LOCKED);
        timeSlotRepository.save(slot);
        Booking saved = bookingRepository.save(booking);
        if (isWalkIn) {
            saved.setQrCode(bookingQrPort.issue(
                    saved.getBookingId(), LocalDateTime.of(saved.getPlayDate(), saved.getEndTime())
            ));
            return bookingRepository.save(saved);
        }
        return saved;
    }

    @Override
    public Booking getBookingById(UUID bookingId) throws ServiceException {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy thông tin đặt sân với ID: " + bookingId));
    }

    @Override
    public PlayerBookingView getPlayerBooking(UUID bookingId, UUID playerId) throws ServiceException {
        Booking booking = getBookingById(bookingId);
        if (booking.getPlayerId() == null || !booking.getPlayerId().equals(playerId)) {
            throw new OwnershipException("Booking không thuộc tài khoản của bạn.");
        }
        return toPlayerView(booking);
    }

    @Override
    public List<Booking> getBookingHistory(UUID playerId, String status, int page, int size) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return bookingRepository.findByPlayerIdAndStatus(playerId, status, page, size);
        }
        return bookingRepository.findByPlayerId(playerId, page, size);
    }

    @Override
    public PageResult<PlayerBookingView> getPlayerBookingHistory(
            UUID playerId, String status, int page, int size
    ) throws ServiceException {
        if (playerId == null) throw new ServiceException("Không xác định được người chơi.");
        if (page < 0 || size < 1 || size > MAX_PLAYER_PAGE_SIZE) {
            throw new ServiceException("Trang phải từ 0 và kích thước trang từ 1 đến 50.");
        }
        BookingStatus parsedStatus = parseStatus(status);
        PageResult<Booking> result = bookingRepository.findPlayerPage(playerId, parsedStatus, page, size);
        return new PageResult<>(
                result.items().stream().map(this::toPlayerView).toList(),
                result.total(), result.page(), result.pageSize(), result.totalPages()
        );
    }

    @Override
    public BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException {
        if (command == null || command.bookingId() == null || command.requestedByUserId() == null
                || command.reason() == null || command.reason().isBlank()) {
            throw new ServiceException("Booking, người yêu cầu và lý do hủy là bắt buộc.");
        }
        if (command.reason().trim().length() > 1000) {
            throw new ServiceException("Lý do hủy không được vượt quá 1000 ký tự.");
        }
        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));

        if (!booking.getPlayerId().equals(command.requestedByUserId())) {
            throw new ServiceException("Bạn không có quyền yêu cầu hủy đơn đặt này.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ServiceException("Trạng thái đơn hiện tại không cho phép yêu cầu hủy.");
        }
        if (bookingCancellationRepository.findByBookingId(booking.getBookingId()).isPresent()) {
            throw new ServiceException("Booking này đã có yêu cầu hủy.");
        }

        VenueCourt court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con của đơn đặt."));
        Venue venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cụm sân của đơn đặt."));

        double refundPercentage = calculateRefundPercentage(venue.getCancellationPolicy(), booking);
        double refundableDeposit = booking.getDepositPaymentId() == null || booking.getDepositAmount() == null
                ? 0.0
                : booking.getDepositAmount();

        BookingCancellation cancellation = BookingCancellation.builder()
                .bookingId(booking.getBookingId())
                .reason(command.reason().trim())
                .refundPercentage(refundPercentage)
                .refundAmount((double) Math.round(refundableDeposit * refundPercentage / 100.0))
                .status(CancellationStatus.PENDING)
                .build();

        return bookingCancellationRepository.save(cancellation);
    }

    @Override
    public BookingCancellation processCancellation(
            UUID cancellationId, boolean approved, UUID processedByUserId, String processNote
    ) throws ServiceException {
        BookingCancellation cancellation = bookingCancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy yêu cầu hủy sân."));

        if (cancellation.getStatus() != CancellationStatus.PENDING) {
            if ((approved && cancellation.getStatus() != CancellationStatus.REJECTED)
                    || (!approved && cancellation.getStatus() == CancellationStatus.REJECTED)) {
                return cancellation;
            }
            throw new ServiceException("Yêu cầu hủy đã được xử lý với quyết định khác.");
        }

        Booking booking = bookingRepository.findByIdForUpdate(cancellation.getBookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));
        VenueCourt court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân của booking."));
        Venue venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở của booking."));
        if (venue.getOwnerId() == null || !venue.getOwnerId().equals(processedByUserId)) {
            throw new OwnershipException("Bạn không có quyền xử lý yêu cầu hủy của booking này.");
        }

        cancellation.setProcessedBy(processedByUserId);
        cancellation.setDecisionReason(processNote);
        cancellation.setProcessedAt(LocalDateTime.now(clock));

        if (!approved) {
            cancellation.setStatus(CancellationStatus.REJECTED);
            return bookingCancellationRepository.save(cancellation);
        }

        if (cancellation.getRefundAmount() != null && cancellation.getRefundAmount() > 0) {
            if (booking.getDepositPaymentId() == null) {
                throw new ServiceException("Booking chưa có giao dịch tiền cọc để hoàn tiền.");
            }
            RefundSnapshot refund = paymentRefundPort.createRefund(
                    booking.getDepositPaymentId(),
                    processedByUserId,
                    "booking-cancellation:" + cancellation.getCancellationId(),
                    refundReason(booking, cancellation),
                    cancellation.getRefundAmount()
            );
            cancellation.setRefundId(refund.refundId());
            applyRefundStatus(refund.status(), cancellation, booking);
        } else {
            cancellation.setStatus(CancellationStatus.APPROVED);
            booking.setStatus(BookingStatus.CANCELLED);
        }
        timeSlotLifecycleService.releaseBookingSlot(booking);
        bookingRepository.save(booking);
        return bookingCancellationRepository.save(cancellation);
    }

    @Override
    public String generateBookingCode() {
        StringBuilder code = new StringBuilder("GS");
        for (int index = 0; index < 6; index++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private double calculateRefundPercentage(CancellationPolicy policy, Booking booking) {
        if (policy == null) return 0.0;
        long hoursBefore = Duration.between(
                LocalDateTime.now(clock), LocalDateTime.of(booking.getPlayDate(), booking.getStartTime())
        ).toHours();
        if (hoursBefore >= policy.getFullRefundHoursBefore()) return 100.0;
        if (hoursBefore >= policy.getPartialRefundHoursBefore()) {
            return policy.getPartialRefundPercentage() == null ? 0.0 : policy.getPartialRefundPercentage();
        }
        return 0.0;
    }

    private BookingStatus parseStatus(String status) throws ServiceException {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return null;
        try {
            return BookingStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("Trạng thái booking không hợp lệ.");
        }
    }

    private PlayerBookingView toPlayerView(Booking booking) {
        VenueCourt court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy sân của booking."));
        Venue venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy cơ sở của booking."));
        BookingCancellation cancellation = bookingCancellationRepository
                .findByBookingId(booking.getBookingId()).orElse(null);
        PlayerReviewView review = reviewRepository.findByBookingId(booking.getBookingId())
                .map(this::toPlayerReviewView)
                .orElse(null);
        return new PlayerBookingView(
                booking, venue.getVenueId(), venue.getName(), court.getName(),
                venue.getCancellationPolicy(), cancellation, review
        );
    }

    private PlayerReviewView toPlayerReviewView(Review review) {
        return new PlayerReviewView(
                review.getReviewId(), review.getVenueId(), review.getBookingId(),
                review.getRating(), review.getContent(), review.getStatus(), review.getCreatedAt()
        );
    }

    private String refundReason(Booking booking, BookingCancellation cancellation) {
        String reason = "Hủy booking " + booking.getBookingCode() + ": " + cancellation.getReason();
        return reason.length() <= 500 ? reason : reason.substring(0, 500);
    }

    private void applyRefundStatus(
            String refundStatus, BookingCancellation cancellation, Booking booking
    ) {
        if ("SUCCEEDED".equals(refundStatus)) {
            cancellation.setStatus(CancellationStatus.REFUNDED);
            booking.setStatus(BookingStatus.REFUNDED);
            return;
        }
        if ("FAILED".equals(refundStatus) || "MANUAL_REVIEW".equals(refundStatus)) {
            cancellation.setStatus(CancellationStatus.REFUND_FAILED);
            booking.setStatus(BookingStatus.REFUND_PENDING);
            return;
        }
        cancellation.setStatus(CancellationStatus.REFUND_PROCESSING);
        booking.setStatus(BookingStatus.REFUND_PENDING);
    }
}
