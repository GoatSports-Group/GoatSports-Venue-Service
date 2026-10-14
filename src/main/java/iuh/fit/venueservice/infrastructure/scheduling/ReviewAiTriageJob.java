package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.port.ContentClassifierPort;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.domain.entity.AiVerdict;
import iuh.fit.venueservice.domain.entity.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Cho AI đọc các đánh giá có chữ để trang Đánh giá của admin lọc được "AI nghi vi phạm". Vài đánh giá mỗi lượt,
 * mới trước, để không vượt hạn mức gọi LLM. AI không dùng được thì dừng lượt và thử lại sau, không tính là lỗi
 * của đánh giá; AI trả lời sai định dạng thì tính một lần thử, quá {@link #MAX_ATTEMPTS} lần thì bỏ qua.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "moderation.ai.enabled", havingValue = "true", matchIfMissing = true)
public class ReviewAiTriageJob {
    static final int MAX_ATTEMPTS = 3;

    private final ReviewRepository reviewRepository;
    private final ContentClassifierPort classifier;

    @Value("${moderation.ai.batch-size:2}")
    private int batchSize = 2;

    static final Duration FIRST_BACKOFF = Duration.ofMinutes(1);
    static final Duration MAX_BACKOFF = Duration.ofMinutes(30);
    private Duration backoff = Duration.ZERO;
    private Instant pausedUntil = Instant.EPOCH;

    @Scheduled(initialDelayString = "${moderation.ai.initial-delay-ms:60000}",
            fixedDelayString = "${moderation.ai.scan-ms:30000}")
    public void triagePending() {
        if (isPaused()) return;
        for (UUID reviewId : reviewRepository.findAwaitingAiVerdict(MAX_ATTEMPTS, batchSize)) {
            Optional<Review> review = reviewRepository.findById(reviewId);
            if (review.isEmpty() || review.get().getContent() == null) continue;
            try {
                Optional<AiVerdict> verdict = classifier.classify("REVIEW", review.get().getContent());
                if (verdict.isEmpty()) {
                    backOff();
                    return;
                }
                backoff = Duration.ZERO;
                reviewRepository.saveAiVerdict(reviewId, verdict.get());
            } catch (ContentClassifierPort.Unclassifiable exception) {
                log.warn("AI could not classify review {}: {}", reviewId, exception.getMessage());
                reviewRepository.addAiAttempts(reviewId, 1);
            }
        }
    }

    private void backOff() {
        backoff = backoff.isZero() ? FIRST_BACKOFF : backoff.multipliedBy(2);
        if (backoff.compareTo(MAX_BACKOFF) > 0) backoff = MAX_BACKOFF;
        pausedUntil = Instant.now().plus(backoff);
        log.info("AI moderation unavailable, pausing for {} min", backoff.toMinutes());
    }

    boolean isPaused() {
        return Instant.now().isBefore(pausedUntil);
    }
}
