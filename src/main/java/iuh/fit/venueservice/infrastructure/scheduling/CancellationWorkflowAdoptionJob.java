package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.service.BookingCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Yêu cầu hủy còn dở từ trước khi chuyển sang Camunda (chưa có process) được mở process, bắt đầu đúng bước nó đang
 * dừng (chờ chủ sân duyệt, chờ khoản hoàn). Từ đó trạng thái của chúng cũng do Camunda quyết định. workflow-service
 * chưa sẵn sàng thì dừng lượt này, lượt sau thử lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationWorkflowAdoptionJob {
    private static final int BATCH = 50;

    private final BookingCancellationService bookingCancellationService;

    @Scheduled(fixedDelayString = "${booking.cancellation-workflow-adoption-ms:300000}", initialDelay = 60_000)
    public void adopt() {
        int adopted = 0;
        for (UUID cancellationId : bookingCancellationService.findCancellationsWithoutWorkflow(BATCH)) {
            try {
                bookingCancellationService.adoptIntoWorkflow(cancellationId);
                adopted++;
            } catch (Exception exception) {
                log.warn("Chưa mở được process cho yêu cầu hủy {}: {}", cancellationId, exception.getMessage());
                break;
            }
        }
        if (adopted > 0) log.info("Đã chuyển {} yêu cầu hủy cũ sang Camunda", adopted);
    }
}
