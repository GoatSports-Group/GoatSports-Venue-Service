package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.ReviewService;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.presentation.request.venue.ModerateReviewRequest;
import iuh.fit.venueservice.presentation.response.venue.ModeratedReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class ReviewAdminController {
    private final ReviewService reviewService;

    @PatchMapping("/{reviewId}/status")
    @PreAuthorize("hasAuthority('MODERATE_REVIEW')")
    @AuditLog(action = "MODERATE_REVIEW", description = "Kiểm duyệt đánh giá #{#reviewId}")
    public ResponseEntity<ModeratedReviewResponse> moderate(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ModerateReviewRequest request
    ) throws ServiceException {
        Review review = reviewService.moderateReview(reviewId, request.getStatus());
        return ResponseEntity.ok(ModeratedReviewResponse.builder()
                .reviewId(review.getReviewId())
                .venueId(review.getVenueId())
                .rating(review.getRating())
                .status(review.getStatus())
                .build());
    }
}
