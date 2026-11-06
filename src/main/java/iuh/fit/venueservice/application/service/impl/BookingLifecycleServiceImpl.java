package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingLifecyclePort;
import iuh.fit.venueservice.application.port.BookingLifecyclePort.StartBookingLifecycle;
import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.service.BookingLifecycleService;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static iuh.fit.venueservice.application.service.BookingPaymentStatusPolicy.isFullyPaid;

/**
 * Transaction do {@code BookingLifecycleDecorator} mở. Mỗi mốc giờ chỉ ghi khi đơn còn đúng ở bước chờ mốc đó, nên
 * process Camunda và job dự phòng cùng gọi cũng không ghi hai lần; trạng thái trả về giúp process rẽ đúng nhánh khi
 * lỡ message (ví dụ đã đóng cọc thì không cho hết hạn).
 */
public class BookingLifecycleServiceImpl implements BookingLifecycleService {
    /** Timer của Camunda và đồng hồ venue-service có thể lệch nhau vài giây. */
    private static final Duration CLOCK_TOLERANCE = Duration.ofSeconds(30);
    private static final Set<BookingStatus> TRACKED = EnumSet.of(
            BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookingRepository bookingRepository;
    private final PaymentQueryPort paymentQueryPort;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final BookingNotificationPort notificationPort;
    private final BookingLifecyclePort lifecyclePort;
    private final Clock clock;

    public BookingLifecycleServiceImpl(
            BookingRepository bookingRepository,
            PaymentQueryPort paymentQueryPort,
            TimeSlotLifecycleService timeSlotLifecycleService,
            BookingNotificationPort notificationPort,
            BookingLifecyclePort lifecyclePort,
            Clock clock
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentQueryPort = paymentQueryPort;
        this.timeSlotLifecycleService = timeSlotLifecycleService;
        this.notificationPort = notificationPort;
        this.lifecyclePort = lifecyclePort;
        this.clock = clock;
    }

    @Override
    public BookingStatus applyTimedState(UUID bookingId, BookingStatus target) throws ServiceException {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        LocalDateTime reached = LocalDateTime.now(clock).plus(CLOCK_TOLERANCE);
        switch (target) {
            case EXPIRED -> expire(booking, reached);
            case NO_SHOW -> markNoShow(booking, reached);
            case COMPLETED -> complete(booking, reached);
            default -> throw new ServiceException("Chỉ ghi được trạng thái theo mốc giờ: EXPIRED, NO_SHOW, COMPLETED.");
        }
        return booking.getStatus();
    }

    @Override
    public List<UUID> findBookingsWithoutLifecycle(int limit) {
        return bookingRepository.findWithoutLifecycle(TRACKED, LocalDateTime.now(clock), limit).stream()
                .map(Booking::getBookingId)
                .toList();
    }

    @Override
    public void adoptIntoLifecycle(UUID bookingId) throws ServiceException {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân."));
        if (booking.getLifecycleInstanceKey() != null || !TRACKED.contains(booking.getStatus())
                || !playEnd(booking).isAfter(LocalDateTime.now(clock))) {
            return;
        }
        Long key = lifecyclePort.start(StartBookingLifecycle.of(booking));
        if (key == null) throw new ServiceException("workflow-service chưa mở được process vòng đời đặt sân.");
        booking.setLifecycleInstanceKey(key);
        bookingRepository.save(booking);
    }

    /** Hết giờ giữ chỗ mà chưa có cọc: nhả khung giờ. */
    private void expire(Booking booking, LocalDateTime reached) throws ServiceException {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getSource() == BookingSource.WALK_IN
                || booking.getHoldExpiresAt() == null
                || booking.getHoldExpiresAt().isAfter(reached)) {
            return;
        }
        booking.setStatus(BookingStatus.EXPIRED);
        timeSlotLifecycleService.releaseBookingSlot(booking);
        bookingRepository.save(booking);
    }

    /**
     * Hết giờ chơi mà chưa quét QR: không đến. Quét QR là cách duy nhất để vào sân, nên cọc không hoàn, khoản còn lại
     * không thu, khung giờ giữ nguyên.
     */
    private void markNoShow(Booking booking, LocalDateTime reached) {
        if (booking.getStatus() != BookingStatus.CONFIRMED || playEnd(booking).isAfter(reached)) return;
        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);
        if (booking.getPlayerId() != null) {
            notificationPort.notifyPayer(booking.getPlayerId(), booking.getBookingId(),
                    "booking-no-show:" + booking.getBookingId(),
                    "Không đến nhận sân",
                    "Lượt đặt " + booking.getBookingCode() + " ngày " + booking.getPlayDate().format(DAY) + " "
                            + booking.getStartTime().toString().substring(0, 5) + "–"
                            + booking.getEndTime().toString().substring(0, 5)
                            + " đã hết giờ mà chưa nhận sân, nên tiền cọc không được hoàn.");
        }
    }

    /** Hết giờ chơi, đã nhận sân và đã thu đủ. */
    private void complete(Booking booking, LocalDateTime reached) {
        if (booking.getStatus() != BookingStatus.CHECKED_IN || playEnd(booking).isAfter(reached)) return;
        if (!isFullyPaid(booking, paymentQueryPort.findByBookingIds(Set.of(booking.getBookingId())))) return;
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    private static LocalDateTime playEnd(Booking booking) {
        return LocalDateTime.of(booking.getPlayDate(), booking.getEndTime());
    }
}
