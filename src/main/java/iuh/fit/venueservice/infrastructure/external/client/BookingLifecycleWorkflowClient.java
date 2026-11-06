package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-WORKFLOW-SERVICE",
        contextId = "bookingLifecycleWorkflow",
        url = "${workflow.service.url}",
        path = "/internal/booking-lifecycles",
        configuration = WorkflowClient.Config.class
)
public interface BookingLifecycleWorkflowClient {
    @PostMapping
    BaseResponse<Map<String, Long>> start(@RequestBody StartRequest request);

    @PostMapping("/{bookingId}/messages")
    void publish(@PathVariable UUID bookingId, @RequestBody MessageRequest request);

    record StartRequest(UUID bookingId, String status, boolean remainingPaid, String holdExpiresAt, String playEndAt) {
    }

    record MessageRequest(String name, String messageId) {
    }
}
