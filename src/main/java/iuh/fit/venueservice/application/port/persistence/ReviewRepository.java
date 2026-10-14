package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.request.venue.AdminReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewRecord;
import iuh.fit.venueservice.domain.entity.AiVerdict;
import iuh.fit.venueservice.domain.entity.Review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    Review save(Review review);

    Optional<Review> findById(UUID reviewId);

    Optional<Review> findByBookingId(UUID bookingId);

    PageResult<OwnerReviewRecord> findOwnedReviews(OwnerReviewFilter filter);

    PageResult<OwnerReviewRecord> findAllReviews(AdminReviewFilter filter);

    PageResult<Review> findPublishedByVenueId(UUID venueId, int page, int size);

    long countPublishedByVenueId(UUID venueId);

    Double averagePublishedRatingByVenueId(UUID venueId);

    List<UUID> findAwaitingAiVerdict(int maxAttempts, int limit);

    void saveAiVerdict(UUID reviewId, AiVerdict verdict);

    void addAiAttempts(UUID reviewId, int attempts);
}
