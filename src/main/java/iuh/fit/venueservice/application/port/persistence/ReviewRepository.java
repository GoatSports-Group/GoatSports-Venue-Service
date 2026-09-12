package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewRecord;
import iuh.fit.venueservice.domain.entity.Review;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    Review save(Review review);

    Optional<Review> findByBookingId(UUID bookingId);

    PageResult<OwnerReviewRecord> findOwnedReviews(OwnerReviewFilter filter);

    PageResult<Review> findPublishedByVenueId(UUID venueId, int page, int size);

    long countPublishedByVenueId(UUID venueId);

    Double averagePublishedRatingByVenueId(UUID venueId);
}
