package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.WorkflowCancellationStateCommand;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.BookingCancellation;

import java.util.List;
import java.util.UUID;

public interface BookingCancellationService {

    BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException;

    /** Chủ sân duyệt/từ chối: quyết định đi vào process, trạng thái đổi khi process ghi lại. */
    BookingCancellation processCancellation(UUID cancellationId, boolean approved, UUID processedByUserId,
                                            String processNote) throws ServiceException;

    /** Chủ sân xác nhận đã chuyển tiền tay cho khoản hoàn mà cổng thanh toán không tự hoàn được. */
    BookingCancellation completeManualRefund(UUID cancellationId, UUID ownerId, String providerRefundId)
            throws ServiceException;

    // ---- Các bước do process trên Camunda gọi ----

    /** Ghi trạng thái Camunda vừa quyết định (bản chiếu) cùng các việc đi kèm: trả khung giờ, đổi trạng thái đơn. */
    BookingCancellation applyWorkflowState(WorkflowCancellationStateCommand command) throws ServiceException;

    /** Tạo khoản hoàn ở payment-service (idempotent theo yêu cầu hủy); không đổi trạng thái. */
    RefundSnapshot createWorkflowRefund(UUID cancellationId, UUID bookingId, UUID processedBy) throws ServiceException;

    /** Nhắc chủ sân còn yêu cầu hủy chờ duyệt. */
    void remindOwnerToDecide(UUID cancellationId, UUID bookingId) throws ServiceException;

    /** Yêu cầu hủy còn dở nhưng chưa có process (tạo trước khi chuyển sang Camunda). */
    List<UUID> findCancellationsWithoutWorkflow(int limit);

    /** Mở process cho yêu cầu hủy cũ, bắt đầu đúng ở bước nó đang dừng. */
    void adoptIntoWorkflow(UUID cancellationId) throws ServiceException;
}
