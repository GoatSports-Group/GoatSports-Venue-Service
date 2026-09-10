package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.ConfirmOwnerCheckInCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateWalkInBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInLookupCommand;
import iuh.fit.venueservice.application.dto.request.venue.PaymentCollectionMode;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCheckInView;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import iuh.fit.venueservice.application.port.PaymentCollectionPort;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.CheckInRecordRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerCheckInService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.CheckInRecord;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CheckInMethod;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

public class OwnerCheckInServiceImpl implements OwnerCheckInService {
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final CheckInRecordRepository checkInRecordRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final PaymentQueryPort paymentQueryPort;
    private final PaymentCollectionPort paymentCollectionPort;
    private final BookingQrPort bookingQrPort;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final Clock clock;
    private final Duration onlineEarlyCheckInWindow;

    public OwnerCheckInServiceImpl(
            BookingRepository bookingRepository,
            CheckInRecordRepository checkInRecordRepository,
            TimeSlotRepository timeSlotRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            PaymentQueryPort paymentQueryPort,
            PaymentCollectionPort paymentCollectionPort,
            BookingQrPort bookingQrPort,
            TimeSlotLifecycleService timeSlotLifecycleService,
            Clock clock,
            Duration onlineEarlyCheckInWindow
    ) {
        this.bookingRepository = bookingRepository;
        this.checkInRecordRepository = checkInRecordRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.paymentQueryPort = paymentQueryPort;
        this.paymentCollectionPort = paymentCollectionPort;
        this.bookingQrPort = bookingQrPort;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
        this.clock = clock;
        this.onlineEarlyCheckInWindow = onlineEarlyCheckInWindow;
    }

    @Override
    public OwnerCheckInView lookup(OwnerCheckInLookupCommand command) throws ServiceException {
        command.validate();
        UUID bookingId = resolveBookingId(command);
        OwnerBookingRecord record = getOwnedBooking(bookingId, command.ownerId(), false);
        List<PaymentSnapshot> payments = loadPayments(bookingId);
        return view(record, payments, null);
    }

    @Override
    public OwnerCheckInView confirm(ConfirmOwnerCheckInCommand command) throws ServiceException {
        command.validate();
        OwnerBookingRecord record = getOwnedBooking(command.bookingId(), command.ownerId(), true);
        Booking booking = record.booking();
        ensureCheckInEligible(booking);
        ensureCheckInEvidenceMatches(command, booking);
        if (checkInRecordRepository.findByBookingId(booking.getBookingId()).isPresent()) {
            throw new ServiceException("Mã vé này đã được dùng để nhận sân trước đó.");
        }

        List<PaymentSnapshot> payments = loadPayments(booking.getBookingId());
        PaymentSnapshot remainingPayment = succeededRemainingPayment(payments, booking.getRemainingAmount());
        BigDecimal remaining = money(booking.getRemainingAmount());
        BigDecimal collectedAtCheckIn = BigDecimal.ZERO;
        if (remaining.signum() > 0 && remainingPayment == null) {
            if (command.paymentMode() == PaymentCollectionMode.ONLINE) {
                throw new ServiceException("Khoản tiền còn lại chưa được dịch vụ thanh toán xác nhận thành công.");
            }
            if (command.paymentMode() == PaymentCollectionMode.CASH) {
                UUID payerId = booking.getPlayerId() == null ? command.ownerId() : booking.getPlayerId();
                remainingPayment = paymentCollectionPort.collectBookingRemaining(
                        booking.getBookingId(), payerId, command.ownerId(), booking.getRemainingAmount()
                );
                if (!"SUCCEEDED".equals(remainingPayment.status())) {
                    throw new ServiceException("Dịch vụ thanh toán chưa xác nhận thu tiền mặt thành công.");
                }
                collectedAtCheckIn = remaining;
                payments = loadPayments(booking.getBookingId());
            }
        }

        CheckInRecord checkIn = checkInRecordRepository.save(CheckInRecord.builder()
                .bookingId(booking.getBookingId())
                .paymentId(remainingPayment == null ? null : remainingPayment.paymentId())
                .checkedInBy(command.ownerId())
                .method(command.method())
                .remainingAmountCollected(collectedAtCheckIn)
                .checkedInAt(LocalDateTime.now(clock))
                .build());
        if (remainingPayment != null) {
            booking.setRemainingPaymentId(remainingPayment.paymentId());
        }
        timeSlotLifecycleService.confirmBookingSlot(booking);
        booking.setStatus(BookingStatus.CHECKED_IN);
        Booking saved = bookingRepository.save(booking);
        return view(new OwnerBookingRecord(
                saved, record.venueId(), record.venueName(), record.courtName()
        ), payments, checkIn);
    }

    @Override
    public OwnerCheckInView createWalkIn(CreateWalkInBookingCommand command) throws ServiceException {
        command.validate();
        VenueCourt court = venueCourtRepository.findById(command.venueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân thi đấu."));
        if (!Boolean.TRUE.equals(court.getActive()) || court.getDeletedAt() != null) {
            throw new ServiceException("Sân đang tạm ngừng hoạt động.");
        }
        Venue venue = getOwnedVenue(court.getVenueId(), command.ownerId());
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(command.timeSlotId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy khung giờ."));
        if (!command.venueCourtId().equals(slot.getVenueCourtId())) {
            throw new ServiceException("Khung giờ không thuộc sân thi đấu đã chọn.");
        }
        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new ServiceException("Khung giờ không còn khả dụng.");
        }
        LocalDateTime end = LocalDateTime.of(slot.getDate(), slot.getEndTime());
        if (end.isBefore(LocalDateTime.now(clock))) {
            throw new ServiceException("Không thể xếp lịch cho khách tại quầy vào khung giờ đã qua.");
        }
        long minutes = Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes();
        if (minutes <= 0 || slot.getPricePerHour() == null || slot.getPricePerHour() <= 0) {
            throw new ServiceException("Khung giờ chưa có thời lượng hoặc giá hợp lệ.");
        }
        double total = BigDecimal.valueOf(slot.getPricePerHour())
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .doubleValue();
        Booking saved = bookingRepository.save(Booking.builder()
                .venueCourtId(court.getVenueCourtId())
                .playDate(slot.getDate()).startTime(slot.getStartTime()).endTime(slot.getEndTime())
                .status(BookingStatus.CONFIRMED).source(BookingSource.WALK_IN)
                .totalPrice(total).depositAmount(0.0).remainingAmount(total)
                .bookingCode(generateBookingCode())
                .walkInCustomerName(command.customerName().trim())
                .walkInCustomerPhone(command.normalizedPhone())
                .build());
        saved.setQrCode(bookingQrPort.issue(saved.getBookingId(), end));
        saved = bookingRepository.save(saved);
        slot.setStatus(TimeSlotStatus.BOOKED);
        timeSlotRepository.save(slot);
        return view(new OwnerBookingRecord(
                saved, venue.getVenueId(), venue.getName(), court.getName()
        ), List.of(), null);
    }

    @Override
    public PageResult<OwnerCheckInView> history(OwnerCheckInFilter filter) throws ServiceException {
        filter.validate();
        ensureHistoryScope(filter);
        var page = checkInRecordRepository.findOwnedCheckIns(filter);
        Set<UUID> bookingIds = page.items().stream()
                .map(item -> item.bookingRecord().booking().getBookingId()).collect(Collectors.toSet());
        Map<UUID, List<PaymentSnapshot>> payments = bookingIds.isEmpty()
                ? Collections.emptyMap()
                : paymentQueryPort.findByBookingIds(bookingIds).stream()
                .collect(Collectors.groupingBy(PaymentSnapshot::referenceId));
        var items = page.items().stream().map(item -> view(
                item.bookingRecord(),
                payments.getOrDefault(item.bookingRecord().booking().getBookingId(), List.of()),
                item.checkInRecord()
        )).toList();
        return new PageResult<>(items, page.total(), page.page(), page.pageSize(), page.totalPages());
    }

    private UUID resolveBookingId(OwnerCheckInLookupCommand command) throws ServiceException {
        if (command.bookingId() != null) return command.bookingId();
        if (hasText(command.qrCode()) == 1) {
            UUID bookingId = bookingQrPort.verify(command.qrCode().trim());
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân để nhận sân."));
            if (booking.getQrCode() == null || !booking.getQrCode().equals(command.qrCode().trim())) {
                throw new ServiceException("Mã QR không còn hiệu lực cho đơn đặt sân này.");
            }
            return bookingId;
        }
        return bookingRepository.findByBookingCode(command.bookingCode().trim().toUpperCase())
                .map(Booking::getBookingId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân để nhận sân."));
    }

    private void ensureCheckInEligible(Booking booking) throws ServiceException {
        String reason = checkInIneligibilityReason(booking);
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    private String checkInIneligibilityReason(Booking booking) {
        if (booking.getStatus() == BookingStatus.CHECKED_IN || booking.getStatus() == BookingStatus.COMPLETED) {
            return "Mã vé này đã được dùng để nhận sân trước đó.";
        }
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REFUND_PENDING
                || booking.getStatus() == BookingStatus.REFUNDED
                || booking.getStatus() == BookingStatus.EXPIRED) {
            return "Đơn đặt sân đã bị hủy hoặc hết hạn, không thể nhận sân.";
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return "Chỉ đơn còn hiệu lực mới được nhận sân.";
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                && booking.getHoldExpiresAt() != null
                && !now.isBefore(booking.getHoldExpiresAt())) {
            return "Đơn đặt sân đã hết thời gian giữ chỗ, không thể nhận sân.";
        }
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                && booking.getSource() != BookingSource.WALK_IN) {
            return "Đơn đặt sân trực tuyến chưa hoàn tất tiền cọc nên chưa thể nhận sân.";
        }
        LocalDateTime start = LocalDateTime.of(booking.getPlayDate(), booking.getStartTime());
        LocalDateTime end = LocalDateTime.of(booking.getPlayDate(), booking.getEndTime());
        boolean walkIn = booking.getSource() == BookingSource.WALK_IN;
        LocalDateTime earliestCheckIn = walkIn ? start : start.minus(onlineEarlyCheckInWindow);
        if (now.isBefore(earliestCheckIn)) {
            return walkIn
                    ? "Chưa đến khung giờ sử dụng."
                    : "Đơn đặt sân trực tuyến chỉ được nhận sân sớm tối đa "
                    + onlineEarlyCheckInWindow.toMinutes() + " phút.";
        }
        if (!now.isBefore(end)) {
            return "Đã quá khung giờ sử dụng.";
        }
        return null;
    }

    private void ensureCheckInEvidenceMatches(ConfirmOwnerCheckInCommand command, Booking booking)
            throws ServiceException {
        switch (command.method()) {
            case QR_CODE -> {
                if (hasText(command.qrCode()) == 0
                        || booking.getQrCode() == null
                        || !booking.getQrCode().equals(command.qrCode().trim())
                        || !booking.getBookingId().equals(bookingQrPort.verify(command.qrCode().trim()))) {
                    throw new ServiceException("Mã QR không hợp lệ cho đơn cần nhận sân.");
                }
            }
            case BOOKING_CODE -> {
                if (hasText(command.bookingCode()) == 0
                        || booking.getBookingCode() == null
                        || !booking.getBookingCode().equalsIgnoreCase(command.bookingCode().trim())) {
                    throw new ServiceException("Mã đặt sân không hợp lệ cho đơn cần nhận sân.");
                }
            }
            case MANUAL -> {
                if (booking.getSource() != BookingSource.WALK_IN) {
                    throw new ServiceException("Xác nhận thủ công chỉ áp dụng cho khách tại quầy.");
                }
            }
        }
    }

    private OwnerBookingRecord getOwnedBooking(UUID bookingId, UUID ownerId, boolean forUpdate)
            throws ServiceException {
        var owned = forUpdate
                ? bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId)
                : bookingRepository.findOwnedBookingById(bookingId, ownerId);
        return owned.orElseThrow(() -> bookingRepository.findById(bookingId).isPresent()
                ? new OwnershipException("Đơn đặt không thuộc cụm sân này.")
                : new ServiceException("Không tìm thấy đơn đặt sân để nhận sân."));
    }

    private List<PaymentSnapshot> loadPayments(UUID bookingId) {
        return paymentQueryPort.findByBookingIds(Set.of(bookingId));
    }

    private PaymentSnapshot succeededRemainingPayment(List<PaymentSnapshot> payments, Double expectedAmount) {
        return payments.stream()
                .filter(payment -> "BOOKING_REMAINING".equals(payment.purpose()))
                .filter(payment -> "SUCCEEDED".equals(payment.status()))
                .filter(payment -> payment.amount() != null && expectedAmount != null
                        && Math.abs(payment.amount() - expectedAmount) < 0.01)
                .findFirst().orElse(null);
    }

    private OwnerCheckInView view(
            OwnerBookingRecord record, List<PaymentSnapshot> payments, CheckInRecord checkIn
    ) {
        BigDecimal due = succeededRemainingPayment(payments, record.booking().getRemainingAmount()) == null
                ? money(record.booking().getRemainingAmount()) : BigDecimal.ZERO;
        String checkInMessage = checkInIneligibilityReason(record.booking());
        return new OwnerCheckInView(record, payments, checkIn, due, checkInMessage == null, checkInMessage);
    }

    private Venue getOwnedVenue(UUID venueId, UUID ownerId) throws ServiceException {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở."));
        if (!ownerId.equals(venue.getOwnerId())) {
            throw new OwnershipException("Cơ sở không thuộc quyền sở hữu của bạn.");
        }
        return venue;
    }

    private void ensureHistoryScope(OwnerCheckInFilter filter) throws ServiceException {
        Venue requestedVenue = filter.venueId() == null
                ? null : getOwnedVenue(filter.venueId(), filter.ownerId());
        if (filter.venueCourtId() != null) {
            VenueCourt court = venueCourtRepository.findById(filter.venueCourtId())
                    .orElseThrow(() -> new ServiceException("Không tìm thấy sân thi đấu."));
            Venue venue = getOwnedVenue(court.getVenueId(), filter.ownerId());
            if (requestedVenue != null && !requestedVenue.getVenueId().equals(venue.getVenueId())) {
                throw new ServiceException("Sân thi đấu không thuộc cơ sở đã chọn.");
            }
        }
    }

    private int hasText(String value) {
        return value != null && !value.isBlank() ? 1 : 0;
    }

    private BigDecimal money(Double amount) {
        return amount == null
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateBookingCode() {
        StringBuilder code = new StringBuilder("GS");
        for (int index = 0; index < 8; index++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }
}
