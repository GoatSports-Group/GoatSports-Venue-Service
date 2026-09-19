package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.infrastructure.external.config.AiServiceFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AI-SERVICE-SEMANTIC-SEARCH",
        url = "${ai.service.url}",
        path = "/api/v1/ai/semantic-search",
        configuration = AiServiceFeignConfiguration.class
)
public interface AiSemanticSearchClient {
    @GetMapping
    List<SemanticSearchResult> search(
            @RequestParam("query") String query,
            @RequestParam("document_type") String documentType,
            @RequestParam("max_results") int maxResults
    );

    @PostMapping("/documents")
    void index(@RequestBody SemanticDocumentRequest request);

    record SemanticSearchResult(UUID source_id, Double score) {
    }

    record SemanticDocumentRequest(
            String document_type,
            UUID source_id,
            String content,
            Map<String, Object> metadata,
            OffsetDateTime source_updated_at
    ) {
    }
}
