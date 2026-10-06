package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.PlayerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.PlayerReviewView;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.persistence.CourtHoldRepository;
import iuh.fit.venueservice.domain.entity.CourtHold;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BookingServiceImpl implements BookingService {
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_PLAYER_PAGE_SIZE = 50;
    /** Đặt qua app phải cọc 30% tổng tiền sân; phần còn lại thu khi đến sân. */
    private static final double DEPOSIT_RATE = 0.3;

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final ReviewRepository reviewRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final BookingQrPort bookingQrPort;
    private final Clock clock;
    private final Duration paymentHoldDuration;
    private final CourtHoldRepository courtHoldRepository;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            BookingCancellationRepository bookingCancellationRepository,
            ReviewRepository reviewRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            BookingQrPort bookingQrPort,
            Clock clock,
            Duration paymentHoldDuration,
            CourtHoldRepository courtHoldRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.bookingCancellationRepository = bookingCancellationRepository;
        this.reviewRepository = reviewRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.bookingQrPort = bookingQrPort;
        this.clock = clock;
        this.paymentHoldDuration = paymentHoldDuration;
        this.courtHoldRepository = courtHoldRepository;
    }

    @Override
    public List<TimeSlotResponseCommand> getAvailableSlots(UUID venueCourtId, LocalDate date) throws ServiceException {
        venueCourtRepository.findById(venueCourtId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con với ID: " + venueCourtId));

        List<CourtHold> holds = courtHoldRepository.findOverlapping(List.of(venueCourtId), date, date);
        // Slot trung gio giai dau hien "TOURNAMENT" de nguoi choi biet vi sao khong dat duoc.
        return timeSlotRepository.findAvailableSlots(venueCourtId, date).stream()
                .map(slot -> new TimeSlotResponseCommand(
                        slot.getTimeSlotId(), slot.getVenueCourtId(), slot.getDate(),
                        slot.getStartTime(), slot.getEndTime(), slot.getPricePerHour(),
                        holds.stream().anyMatch(hold -> hold.covers(date, slot.getStartTime(), slot.getEndTime()))
                                ? "TOURNAMENT" : slot.getStatus().name()
                ))
                .toList();
    }

    /**
     * Đặt sân. Giá do server tính từ khung giờ (không nhận giá từ client). Khách qua app phải đóng cọc 30% và khung giờ
     * bị giữ (LOCKED) trong {@code paymentHoldDuration}; quá hạn chưa cọc thì job trả khung giờ. Khách tại quầy
     * (WALK_IN) được xác nhận ngay và nhận mã QR check-in.
     */
    @Override
    public Booking createBooking(CreateBookingCommand command) throws ServiceException {
        VenueCourt court = venueCourtRepository.findById(command.venueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân con."));

        if (!Boolean.TRUE.equals(court.getActive()) || court.getDeletedAt() != null) {
            throw new ServiceException("Sân đang tạm ngừng hoạt động.");
        }
        // Cơ sở tắt hoặc bị admin đình chỉ thì không nhận đơn mới, kể cả khi từng sân con vẫn bật.
        if (venueRepository.findById(court.getVenueId()).map(Venue::getActive).filter(Boolean.TRUE::equals).isEmpty()) {
            throw new ServiceException("Cơ sở đang tạm ngừng nhận đặt sân.");
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

        // Khóa hàng time_slot (SELECT ... FOR UPDATE): hai người đặt cùng khung giờ cùng lúc thì chỉ một người thắng.
        TimeSlot slot = timeSlotRepository.findByCourtAndTimeForUpdate(
                        command.venueCourtId(), command.playDate(), command.startTime(), command.endTime()
                )
                .orElseThrow(() -> new ServiceException("Khung giờ chưa được cấu hình."));

        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new ServiceException("Khung giờ không còn khả dụng.");
        }
        // Đọc lịch giữ sân của giải SAU khi đã khóa slot: nếu chủ sân vừa giữ sân cho giải thì lệnh giữ đã xong trước.
        // Đọc từ court_holds (không dựa vào trạng thái slot) nên cả slot sinh sau khi giữ cũng bị chặn.
        if (courtHoldRepository.isHeld(command.venueCourtId(), command.playDate(), command.startTime(), command.endTime())) {
            throw new ServiceException("Sân đang được dùng cho giải đấu trong khung giờ này, vui lòng chọn giờ hoặc sân khác.");
        }

        if (slot.getPricePerHour() == null) {
            throw new ServiceException("Khung giờ chưa có giá hợp lệ.");
        }

        double totalPrice = durationHours * slot.getPricePerHour();
        BookingSource source = command.source() == null ? BookingSource.DIRECT : command.source();
        boolean isWalkIn = source == BookingSource.WALK_IN;
        double depositAmount = isWalkIn ? 0.0 : totalPrice * DEPOSIT_RATE;

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
                ? TimeSlotStatus.BOOKED
                : TimeSlotStatus.LOCKED);
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
    public Booking getMatchmakingBooking(UUID matchmakingSessionId) throws ServiceException {
        return bookingRepository.findLatestByMatchmakingSessionId(matchmakingSessionId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy booking của phiên ghép trận."));
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
        // "Sap dien ra" xet theo gio choi, khong chi trang thai: luot qua gio ma chua dong van khong lot vao.
        PageResult<Booking> result = UPCOMING.equalsIgnoreCase(status == null ? "" : status.trim())
                ? bookingRepository.findPlayerUpcomingPage(playerId, LocalDateTime.now(clock), page, size)
                : bookingRepository.findPlayerPage(playerId, parseStatus(status), page, size);
        return new PageResult<>(
                result.items().stream().map(this::toPlayerView).toList(),
                result.total(), result.page(), result.pageSize(), result.totalPages()
        );
    }

    @Override
    public String generateBookingCode() {
        StringBuilder code = new StringBuilder("GS");
        for (int index = 0; index < 6; index++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    /** Bo loc lich su: luot con hieu luc chua toi gio ket thuc (khong phai mot BookingStatus). */
    static final String UPCOMING = "UPCOMING";

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


}
