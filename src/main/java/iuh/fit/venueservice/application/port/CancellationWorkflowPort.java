package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

/** Process "Hủy đặt sân và hoàn tiền" ở workflow-service (Camunda) — nơi quyết định trạng thái của yêu cầu hủy. */
public interface CancellationWorkflowPort {
    /** @return process instance key; ném lỗi khi workflow-service không mở được process */
    long start(StartCancellationWorkflow command) throws ServiceException;

    /** @return false khi process không còn chờ chủ sân duyệt */
    boolean decide(UUID cancellationId, boolean approved, UUID processedBy, String decisionNote) throws ServiceException;

    /**
     * @param playStartAt ISO-8601 có offset: tới giờ này mà chủ sân chưa duyệt thì process tự chấp thuận
     * @param resumeStatus chỉ có khi nhận nuôi yêu cầu hủy cũ (tạo trước khi có process)
     */
    record StartCancellationWorkflow(
            UUID cancellationId,
            UUID bookingId,
            String reviewMode,
            double refundAmount,
            String playStartAt,
            String decisionNote,
            String resumeStatus,
            UUID refundId
    ) {
    }
}
