package iuh.fit.venueservice.infrastructure.external.adapter;

import feign.FeignException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.CancellationWorkflowPort;
import iuh.fit.venueservice.infrastructure.external.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationWorkflowAdapter implements CancellationWorkflowPort {
    private static final String UNAVAILABLE = "Hệ thống xử lý hủy sân đang bận. Vui lòng thử lại sau ít phút.";

    private final WorkflowClient workflowClient;

    @Override
    public long start(StartCancellationWorkflow command) throws ServiceException {
        try {
            var response = workflowClient.start(new WorkflowClient.StartRequest(
                    command.cancellationId(), command.bookingId(), command.reviewMode(), command.refundAmount(),
                    command.playStartAt(), command.decisionNote(), command.resumeStatus(), command.refundId()));
            Long key = response == null || response.getData() == null ? null : response.getData().get("processInstanceKey");
            if (key == null) throw new IllegalStateException("workflow-service không trả về process instance key");
            return key;
        } catch (FeignException | IllegalStateException exception) {
            log.warn("Không mở được process hủy sân cho yêu cầu {}: {}", command.cancellationId(), exception.getMessage());
            throw new ServiceException(UNAVAILABLE);
        }
    }

    @Override
    public boolean decide(UUID cancellationId, boolean approved, UUID processedBy, String decisionNote)
            throws ServiceException {
        try {
            workflowClient.decide(cancellationId, new WorkflowClient.DecisionRequest(approved, processedBy, decisionNote));
            return true;
        } catch (FeignException.Conflict exception) {
            return false;
        } catch (FeignException exception) {
            log.warn("Không chuyển được quyết định hủy sân {} vào process: {}", cancellationId, exception.getMessage());
            throw new ServiceException(UNAVAILABLE);
        }
    }
}
