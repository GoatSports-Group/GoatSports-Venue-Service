package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.WorkflowCancellationStateCommand;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.service.BookingCancellationService;
import iuh.fit.venueservice.application.service.impl.BookingCancellationServiceImpl;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.enums.CancellationReviewMode;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Bọc use case hủy đơn trong transaction: tầng application không phụ thuộc Spring nên ranh giới giao dịch đặt ở đây.
 *
 * <p>Trạng thái đổi bất đồng bộ (Camunda ghi lại sau khi nhận quyết định), nên các thao tác của người dùng chờ thêm
 * một chút SAU khi commit để trả về trạng thái mới; quá hạn thì trả trạng thái hiện tại, giao diện tải lại sau.
 */
@Service
@Primary
@RequiredArgsConstructor
public class BookingCancellationDecorator implements BookingCancellationService {
    private static final Duration DECISION_SETTLE = Duration.ofSeconds(3);
    /** Xác nhận hoàn tay đi vòng payment-service → Kafka (outbox ~2 giây) → workflow-service → venue-service. */
    private static final Duration MANUAL_REFUND_SETTLE = Duration.ofSeconds(8);
    private static final long POLL_MS = 150;

    private final BookingCancellationServiceImpl delegate;
    private final BookingCancellationRepository cancellationRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException {
        BookingCancellation created = inTransaction(() -> delegate.requestCancellation(command));
        return created.getReviewMode() == CancellationReviewMode.AUTOMATIC
                ? awaitStatusChange(created, DECISION_SETTLE) : created;
    }

    @Override
    public BookingCancellation processCancellation(UUID cancellationId, boolean approved, UUID processedByUserId,
                                                   String processNote) throws ServiceException {
        return awaitStatusChange(inTransaction(() ->
                delegate.processCancellation(cancellationId, approved, processedByUserId, processNote)), DECISION_SETTLE);
    }

    @Override
    public BookingCancellation completeManualRefund(UUID cancellationId, UUID ownerId, String providerRefundId)
            throws ServiceException {
        return awaitStatusChange(inTransaction(() ->
                delegate.completeManualRefund(cancellationId, ownerId, providerRefundId)), MANUAL_REFUND_SETTLE);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public BookingCancellation applyWorkflowState(WorkflowCancellationStateCommand command) throws ServiceException {
        return delegate.applyWorkflowState(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public RefundSnapshot createWorkflowRefund(UUID cancellationId, UUID bookingId, UUID processedBy)
            throws ServiceException {
        return delegate.createWorkflowRefund(cancellationId, bookingId, processedBy);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = ServiceException.class)
    public void remindOwnerToDecide(UUID cancellationId, UUID bookingId) throws ServiceException {
        delegate.remindOwnerToDecide(cancellationId, bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findCancellationsWithoutWorkflow(int limit) {
        return delegate.findCancellationsWithoutWorkflow(limit);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void adoptIntoWorkflow(UUID cancellationId) throws ServiceException {
        delegate.adoptIntoWorkflow(cancellationId);
    }

    /** Đọc lại (mỗi lần một transaction mới) tới khi trạng thái khác lúc vừa commit hoặc hết thời gian chờ. */
    private BookingCancellation awaitStatusChange(BookingCancellation committed, Duration timeout) {
        CancellationStatus from = committed.getStatus();
        long deadline = System.nanoTime() + timeout.toNanos();
        BookingCancellation current = committed;
        while (current.getStatus() == from && System.nanoTime() < deadline) {
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return current;
            }
            current = cancellationRepository.findById(committed.getCancellationId()).orElse(current);
        }
        return current;
    }

    private BookingCancellation inTransaction(ServiceCall call) throws ServiceException {
        try {
            return transactionTemplate.execute(status -> {
                try {
                    return call.run();
                } catch (ServiceException exception) {
                    throw new RollbackSignal(exception);
                }
            });
        } catch (RollbackSignal signal) {
            throw signal.original;
        }
    }

    @FunctionalInterface
    private interface ServiceCall {
        BookingCancellation run() throws ServiceException;
    }

    /** ServiceException là checked: gói lại để TransactionTemplate rollback (như rollbackFor cũ) rồi ném lại nguyên bản. */
    private static final class RollbackSignal extends RuntimeException {
        private final ServiceException original;

        private RollbackSignal(ServiceException original) {
            super(original);
            this.original = original;
        }
    }
}
