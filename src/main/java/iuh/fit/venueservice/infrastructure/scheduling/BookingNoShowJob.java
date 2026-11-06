package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.service.BookingLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Dự phòng cho timer "Hết giờ chơi mà chưa nhận sân" của process "Vòng đời đặt sân": chỉ xử lý đơn đã quá mốc giờ thêm
 * {@code booking.lifecycle-fallback-grace} (process chậm, chưa có process, workflow-service tắt). Ghi qua cùng
 * {@link BookingLifecycleService} với process nên không ghi hai lần.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.no-show-enabled", havingValue = "true", matchIfMissing = true)
public class BookingNoShowJob {
    private final BookingRepository bookingRepository;
    private final BookingLifecycleService bookingLifecycleService;
    private final Clock clock;

    @Value("${booking.no-show-batch-size:100}")
    private int batchSize = 100;

    @Value("${booking.lifecycle-fallback-grace:PT5M}")
    private Duration fallbackGrace = Duration.ofMinutes(5);

    @Scheduled(fixedDelayString = "${booking.no-show-scan-ms:60000}")
    @Transactional(rollbackFor = ServiceException.class)
    public void markNoShows() throws ServiceException {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(fallbackGrace);
        for (Booking booking : bookingRepository.findNoShowCandidatesForUpdate(cutoff, batchSize)) {
            bookingLifecycleService.applyTimedState(booking.getBookingId(), BookingStatus.NO_SHOW);
        }
    }
}
