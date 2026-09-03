package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.request.venue.CreateOwnerBookingPaymentCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingPaymentMethod;
import iuh.fit.venueservice.application.dto.request.venue.UpdateOwnerBookingStatusCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.BookingCheckoutSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingPaymentResult;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.PaymentCollectionPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerBookingService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OwnerBookingServiceImpl implements OwnerBookingService {
    private static final int MAX_PAGE_SIZE = 20;
    private static final int MAX_DATE_RANGE_DAYS = 366;

    private final BookingRepository bookingRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final PaymentQueryPort paymentQueryPort;
    private final PaymentCollectionPort paymentCollectionPort;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final Clock clock;
    private final Duration paymentCheckoutDuration;

    public OwnerBookingServiceImpl(
            BookingRepository bookingRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            PaymentQueryPort paymentQueryPort,
            PaymentCollectionPort paymentCollectionPort,
            TimeSlotLifecycleService timeSlotLifecycleService,
            Clock clock,
            Duration paymentCheckoutDuration
    ) {
        this.bookingRepository = bookingRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.paymentQueryPort = paymentQueryPort;
        this.paymentCollectionPort = paymentCollectionPort;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
        this.clock = clock;
        this.paymentCheckoutDuration = paymentCheckoutDuration;
    }

    @Override
    public PageResult<OwnerBookingView> getBookings(OwnerBookingFilter filter) throws ServiceException {
        validateFilter(filter);
        validateRequestedScope(filter);
        PageResult<OwnerBookingRecord> page = bookingRepository.findOwnedBookings(filter);
        Map<UUID, List<PaymentSnapshot>> payments = loadPayments(
                page.items().stream().map(item -> item.booking().getBookingId()).collect(Collectors.toSet())
        );
        List<OwnerBookingView> items = page.items().stream()
                .map(item -> toView(item, payments.getOrDefault(
                        item.booking().getBookingId(), Collections.emptyList()
                )))
                .toList();
        return new PageResult<>(items, page.total(), page.page(), page.pageSize(), page.totalPages());
    }

    @Override
    public OwnerBookingView getBooking(UUID bookingId, UUID ownerId) throws ServiceException {
        OwnerBookingRecord record = getOwnedBooking(bookingId, ownerId);
        List<PaymentSnapshot> payments = paymentQueryPort.findByBookingIds(Set.of(bookingId));
        return toView(record, payments);
    }

    @Override
    public OwnerBookingView updateStatus(UpdateOwnerBookingStatusCommand command) throws ServiceException {
        if (command == null || command.bookingId() == null || command.ownerId() == null
                || command.status() == null) {
            throw new ServiceException("Booking, trạng thái và owner là bắt buộc.");
        }
        OwnerBookingRecord record = getOwnedBookingForUpdate(command.bookingId(), command.ownerId());
        Booking booking = record.booking();
        if (command.status() != BookingStatus.COMPLETED) {
            throw new ServiceException("Chủ sân không được cập nhật trạng thái thanh toán hoặc trạng thái hệ thống.");
        }
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new ServiceException("Chỉ booking đã check-in mới được chuyển sang hoàn tất.");
        }
        LocalDateTime playEnd = LocalDateTime.of(booking.getPlayDate(), booking.getEndTime());
        if (playEnd.isAfter(LocalDateTime.now(clock))) {
            throw new ServiceException("Chưa thể hoàn tất booking trước giờ kết thúc.");
        }

        List<PaymentSnapshot> payments = paymentQueryPort.findByBookingIds(Set.of(command.bookingId()));
        if (!isFullyPaid(booking, payments)) {
            throw new ServiceException("Booking vẫn còn công nợ nên chưa thể chuyển sang hoàn thành.");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);
        return toView(new OwnerBookingRecord(
                saved, record.venueId(), record.venueName(), record.courtName()
        ), payments);
    }

    @Override
    public OwnerBookingPaymentResult createPayment(CreateOwnerBookingPaymentCommand command) throws ServiceException {
        if (command == null || command.bookingId() == null || command.ownerId() == null
                || command.method() == null) {
            throw new ServiceException("Booking, owner và phương thức thanh toán là bắt buộc.");
        }
        OwnerBookingRecord record = getOwnedBookingForUpdate(command.bookingId(), command.ownerId());
        Booking booking = record.booking();
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.CHECKED_IN
                && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ServiceException("Trạng thái booking không cho phép thu khoản còn lại.");
        }
        if (booking.getRemainingAmount() == null || booking.getRemainingAmount() <= 0) {
            throw new ServiceException("Booking không có khoản tiền cần thu.");
        }
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT
                && booking.getHoldExpiresAt() != null
                && !booking.getHoldExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new ServiceException("Thời gian giữ chỗ đã hết. Vui lòng tạo booking mới.");
        }

        List<PaymentSnapshot> payments = paymentQueryPort.findByBookingIds(Set.of(command.bookingId()));
        PaymentSnapshot succeeded = payments.stream()
                .filter(payment -> "BOOKING_REMAINING".equals(payment.purpose()))
                .filter(payment -> "SUCCEEDED".equals(payment.status()))
                .findFirst().orElse(null);
        if (succeeded != null) {
            applySuccessfulRemainingPayment(booking, succeeded.paymentId());
            return new OwnerBookingPaymentResult(
                    booking.getBookingId(), succeeded.paymentId(), command.method(),
                    succeeded.status(), null, null, null, succeeded.expiresAt()
            );
        }

        boolean pendingOnlinePayment = payments.stream()
                .filter(payment -> "BOOKING_REMAINING".equals(payment.purpose()))
                .anyMatch(payment -> "CREATED".equals(payment.status())
                        || "PENDING".equals(payment.status()));
        if (command.method() == OwnerBookingPaymentMethod.CASH && pendingOnlinePayment) {
            throw new ServiceException(
                    "Booking đang có giao dịch payOS chờ xác nhận. Hãy tiếp tục giao dịch hiện tại; "
                            + "không thể chuyển sang tiền mặt để tránh thu tiền hai lần."
            );
        }

        if (command.method() == OwnerBookingPaymentMethod.CASH) {
            UUID payerId = booking.getPlayerId() == null ? command.ownerId() : booking.getPlayerId();
            PaymentSnapshot payment = paymentCollectionPort.collectBookingRemaining(
                    booking.getBookingId(), payerId, command.ownerId(), booking.getRemainingAmount()
            );
            if (!"SUCCEEDED".equals(payment.status())) {
                throw new ServiceException("Payment-service chưa xác nhận thu tiền mặt thành công.");
            }
            applySuccessfulRemainingPayment(booking, payment.paymentId());
            return new OwnerBookingPaymentResult(
                    booking.getBookingId(), payment.paymentId(), command.method(),
                    payment.status(), null, null, null, payment.expiresAt()
            );
        }

        LocalDateTime checkoutExpiresAt = booking.getHoldExpiresAt() != null
                && booking.getHoldExpiresAt().isAfter(LocalDateTime.now(clock))
                ? booking.getHoldExpiresAt()
                : LocalDateTime.now(clock).plus(paymentCheckoutDuration);
        BookingCheckoutSnapshot checkout = paymentCollectionPort.createBookingCheckout(
                booking.getBookingId(), booking.getPlayerId() == null ? command.ownerId() : booking.getPlayerId(),
                booking.getRemainingAmount(),
                checkoutExpiresAt
        );
        return new OwnerBookingPaymentResult(
                booking.getBookingId(), checkout.paymentId(), command.method(),
                checkout.status(), checkout.checkoutUrl(), checkout.deeplink(),
                checkout.qrCodeContent(), checkout.expiresAt()
        );
    }

    private void applySuccessfulRemainingPayment(Booking booking, UUID paymentId) throws ServiceException {
        booking.setRemainingPaymentId(paymentId);
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CONFIRMED);
            timeSlotLifecycleService.confirmBookingSlot(booking);
        }
        bookingRepository.save(booking);
    }

    private void validateFilter(OwnerBookingFilter filter) throws ServiceException {
        if (filter == null || filter.ownerId() == null) {
            throw new ServiceException("Không xác định được chủ sân.");
        }
        if (filter.page() < 0 || filter.size() < 1 || filter.size() > MAX_PAGE_SIZE) {
            throw new ServiceException("Trang phải từ 0 và kích thước trang từ 1 đến 20.");
        }
        if (filter.fromDate() != null && filter.toDate() != null) {
            if (filter.fromDate().isAfter(filter.toDate())) {
                throw new ServiceException("Khoảng ngày booking không hợp lệ.");
            }
            if (java.time.temporal.ChronoUnit.DAYS.between(
                    filter.fromDate(), filter.toDate()
            ) > MAX_DATE_RANGE_DAYS) {
                throw new ServiceException("Khoảng ngày booking tối đa 366 ngày.");
            }
        }
        if (filter.query() != null && filter.query().trim().length() > 100) {
            throw new ServiceException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
    }

    private void validateRequestedScope(OwnerBookingFilter filter) throws ServiceException {
        Venue requestedVenue = null;
        if (filter.venueId() != null) {
            requestedVenue = getOwnedVenue(filter.venueId(), filter.ownerId());
        }
        if (filter.venueCourtId() != null) {
            VenueCourt court = venueCourtRepository.findById(filter.venueCourtId())
                    .orElseThrow(() -> new ServiceException("Không tìm thấy sân thi đấu."));
            Venue venue = getOwnedVenue(court.getVenueId(), filter.ownerId());
            if (requestedVenue != null && !requestedVenue.getVenueId().equals(venue.getVenueId())) {
                throw new ServiceException("Sân thi đấu không thuộc cơ sở đã chọn.");
            }
        }
    }

    private OwnerBookingRecord getOwnedBooking(UUID bookingId, UUID ownerId) throws ServiceException {
        if (bookingId == null || ownerId == null) {
            throw new ServiceException("Booking và owner là bắt buộc.");
        }
        return bookingRepository.findOwnedBookingById(bookingId, ownerId)
                .orElseThrow(() -> {
                    boolean exists = bookingRepository.findById(bookingId).isPresent();
                    return exists
                            ? new OwnershipException("Booking không thuộc Venue của bạn.")
                            : new ServiceException("Không tìm thấy booking.");
                });
    }

    private OwnerBookingRecord getOwnedBookingForUpdate(UUID bookingId, UUID ownerId) throws ServiceException {
        if (bookingId == null || ownerId == null) {
            throw new ServiceException("Booking và owner là bắt buộc.");
        }
        return bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId)
                .orElseThrow(() -> bookingRepository.findById(bookingId).isPresent()
                        ? new OwnershipException("Booking không thuộc Venue của bạn.")
                        : new ServiceException("Không tìm thấy booking."));
    }

    private Venue getOwnedVenue(UUID venueId, UUID ownerId) throws ServiceException {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở."));
        if (venue.getOwnerId() == null || !venue.getOwnerId().equals(ownerId)) {
            throw new OwnershipException("Cơ sở không thuộc quyền sở hữu của bạn.");
        }
        return venue;
    }

    private Map<UUID, List<PaymentSnapshot>> loadPayments(Set<UUID> bookingIds) {
        if (bookingIds.isEmpty()) return Collections.emptyMap();
        return paymentQueryPort.findByBookingIds(bookingIds).stream()
                .collect(Collectors.groupingBy(PaymentSnapshot::referenceId));
    }

    private OwnerBookingView toView(OwnerBookingRecord record, List<PaymentSnapshot> payments) {
        return new OwnerBookingView(
                record.booking(), record.venueId(), record.venueName(), record.courtName(),
                payments, allowedTransitions(record.booking(), payments)
        );
    }

    private List<BookingStatus> allowedTransitions(Booking booking, List<PaymentSnapshot> payments) {
        if (booking.getStatus() == BookingStatus.CHECKED_IN
                && !LocalDateTime.of(booking.getPlayDate(), booking.getEndTime()).isAfter(LocalDateTime.now(clock))
                && isFullyPaid(booking, payments)) {
            return List.of(BookingStatus.COMPLETED);
        }
        return List.of();
    }

    private boolean isFullyPaid(Booking booking, List<PaymentSnapshot> payments) {
        if (booking.getRemainingAmount() == null || booking.getRemainingAmount() <= 0) return true;
        if (booking.getRemainingPaymentId() != null) return true;
        return payments.stream()
                .filter(payment -> "BOOKING_REMAINING".equals(payment.purpose()))
                .filter(payment -> "SUCCEEDED".equals(payment.status()))
                .anyMatch(payment -> payment.amount() != null
                        && Math.abs(payment.amount() - booking.getRemainingAmount()) < 0.01);
    }
}
