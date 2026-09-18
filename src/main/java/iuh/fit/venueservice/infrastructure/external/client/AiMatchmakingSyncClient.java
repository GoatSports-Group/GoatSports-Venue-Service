package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.infrastructure.external.config.AiServiceFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AI-SERVICE-MATCHMAKING-SYNC",
        url = "${ai.service.url}",
        path = "/api/v1/ai/matchmaking/sessions",
        configuration = AiServiceFeignConfiguration.class
)
public interface AiMatchmakingSyncClient {
    @PostMapping("/{sessionId}/lifecycle-sync")
    void syncLifecycle(
            @PathVariable UUID sessionId,
            @RequestBody LifecycleSyncRequest request
    );

    record LifecycleSyncRequest(UUID bookingId, String status) {
    }
}
