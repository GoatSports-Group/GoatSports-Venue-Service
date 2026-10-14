package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.infrastructure.external.config.AiServiceFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Tên riêng để không dùng chung readTimeout 5s của semantic search: LLM có thể mất vài giây. */
@FeignClient(
        name = "GOATSPORTS-AI-SERVICE-MODERATION",
        url = "${ai.service.url}",
        path = "/api/v1/ai/moderation",
        configuration = AiServiceFeignConfiguration.class
)
public interface AiModerationClient {
    @PostMapping("/classify")
    Verdict classify(@RequestBody ClassifyRequest request);

    record ClassifyRequest(String kind, String text) {
    }

    record Verdict(String label, Integer severity, String reason) {
    }
}
