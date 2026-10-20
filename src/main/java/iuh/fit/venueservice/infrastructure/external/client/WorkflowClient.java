package iuh.fit.venueservice.infrastructure.external.client;

import feign.RequestInterceptor;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-WORKFLOW-SERVICE",
        url = "${workflow.service.url}",
        path = "/internal/booking-cancellations",
        configuration = WorkflowClient.Config.class
)
public interface WorkflowClient {
    @PostMapping
    BaseResponse<Map<String, Long>> start(@RequestBody StartRequest request);

    @PostMapping("/{cancellationId}/decision")
    void decide(@PathVariable UUID cancellationId, @RequestBody DecisionRequest request);

    record StartRequest(UUID cancellationId, UUID bookingId, String reviewMode, double refundAmount,
                        String playStartAt, String decisionNote, String resumeStatus, UUID refundId) {
    }

    record DecisionRequest(boolean approved, UUID processedBy, String decisionNote) {
    }

    class Config {
        @Bean
        RequestInterceptor workflowServiceIdentity(@Value("${app.jwt.secret}") String gatewaySecret) {
            return template -> template.header("X-Gateway-Secret", gatewaySecret);
        }
    }
}
