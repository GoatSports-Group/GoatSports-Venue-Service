package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.port.ContentClassifierPort;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.domain.entity.AiVerdict;
import iuh.fit.venueservice.domain.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewAiTriageJobTest {
    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final ContentClassifierPort classifier = mock(ContentClassifierPort.class);
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(reviews.findAwaitingAiVerdict(ReviewAiTriageJob.MAX_ATTEMPTS, 2)).thenReturn(List.of(first, second));
        when(reviews.findById(first)).thenReturn(Optional.of(Review.builder().reviewId(first).content("Sân bẩn").build()));
        when(reviews.findById(second)).thenReturn(Optional.of(Review.builder().reviewId(second).content("Đồ lừa đảo").build()));
    }

    @Test
    void savesVerdictsAndCountsOneAttemptForUnreadableAnswers() {
        AiVerdict fine = new AiVerdict("NONE", 0, "Chê sân là ý kiến hợp lệ.", LocalDateTime.now());
        when(classifier.classify("REVIEW", "Sân bẩn")).thenReturn(Optional.of(fine));
        when(classifier.classify("REVIEW", "Đồ lừa đảo")).thenThrow(new ContentClassifierPort.Unclassifiable("bad json"));

        new ReviewAiTriageJob(reviews, classifier).triagePending();

        verify(reviews).saveAiVerdict(first, fine);
        verify(reviews).addAiAttempts(second, 1);
    }

    @Test
    void stopsWithoutCountingAttemptsWhenAiIsUnavailable() {
        when(classifier.classify(any(), any())).thenReturn(Optional.empty());

        ReviewAiTriageJob job = new ReviewAiTriageJob(reviews, classifier);
        job.triagePending();
        job.triagePending();

        // Lần hai đang trong thời gian lùi nên không gọi AI lần nữa.
        verify(classifier, times(1)).classify("REVIEW", "Sân bẩn");
        verify(reviews, never()).findById(second);
        verify(reviews, never()).saveAiVerdict(any(), any());
        verify(reviews, never()).addAiAttempts(any(), anyInt());
    }
}
