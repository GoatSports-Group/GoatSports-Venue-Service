package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dong cac luot "Da xac nhan" da qua gio ket thuc ma chua duoc quet QR: chuyen sang NO_SHOW (Khong den nhan san).
 * Quet QR la cach duy nhat de vao san, nen het gio ma chua quet thi chac chan khach khong den. Coc khong hoan,
 * khoan con lai khong thu, khung gio giu nguyen (khong nha). Lan chay dau tien dong luon cac luot dang treo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "booking.no-show-enabled", havingValue = "true", matchIfMissing = true)
public class BookingNoShowJob {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookingRepository bookingRepository;
    private final BookingNotificationPort notificationPort;
    private final Clock clock;

    @Value("${booking.no-show-batch-size:100}")
    private int batchSize = 100;

    @Scheduled(fixedDelayString = "${booking.no-show-scan-ms:60000}")
    @Transactional
    public void markNoShows() {
        LocalDateTime now = LocalDateTime.now(clock);
        int marked = 0;
        for (Booking booking : bookingRepository.findNoShowCandidatesForUpdate(now, batchSize)) {
            if (booking.getStatus() != BookingStatus.CONFIRMED
                    || LocalDateTime.of(booking.getPlayDate(), booking.getEndTime()).isAfter(now)) {
                continue;
            }
            booking.setStatus(BookingStatus.NO_SHOW);
            bookingRepository.save(booking);
            marked++;
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
        if (marked > 0) log.info("Marked {} confirmed bookings as no-show", marked);
    }
}
