package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.service.BookingLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Đơn còn chạy mà chưa có process "Vòng đời đặt sân" (đặt lúc workflow-service chưa sẵn sàng, hoặc đặt trước khi có
 * process) được mở process, bắt đầu đúng bước đơn đang đứng. workflow-service chưa sẵn sàng thì dừng lượt này.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLifecycleAdoptionJob {
    private static final int BATCH = 50;

    private final BookingLifecycleService bookingLifecycleService;

    @Scheduled(fixedDelayString = "${booking.lifecycle-adoption-ms:60000}", initialDelay = 45_000)
    public void adopt() {
        int adopted = 0;
        for (UUID bookingId : bookingLifecycleService.findBookingsWithoutLifecycle(BATCH)) {
            try {
                bookingLifecycleService.adoptIntoLifecycle(bookingId);
                adopted++;
            } catch (Exception exception) {
                log.warn("Chưa mở được process vòng đời cho đơn {}: {}", bookingId, exception.getMessage());
                break;
            }
        }
        if (adopted > 0) log.info("Đã mở process vòng đời cho {} đơn đặt sân", adopted);
    }
}
