package iuh.fit.venueservice.infrastructure.external.adapter;

import feign.FeignException;
import iuh.fit.venueservice.application.port.ContentClassifierPort;
import iuh.fit.venueservice.domain.entity.AiVerdict;
import iuh.fit.venueservice.infrastructure.external.client.AiModerationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentClassifierAdapter implements ContentClassifierPort {
    private final AiModerationClient client;

    @Override
    public Optional<AiVerdict> classify(String kind, String text) {
        try {
            AiModerationClient.Verdict verdict = client.classify(new AiModerationClient.ClassifyRequest(kind, text));
            return Optional.of(new AiVerdict(
                    verdict.label(), verdict.severity(), verdict.reason(), LocalDateTime.now()));
        } catch (FeignException.UnprocessableEntity | FeignException.BadRequest exception) {
            throw new Unclassifiable(exception.getMessage());
        } catch (FeignException exception) {
            log.warn("AI moderation unavailable (status {}), retrying on the next scan", exception.status());
            return Optional.empty();
        }
    }
}
