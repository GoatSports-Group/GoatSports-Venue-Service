package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueClaimReview;

/** Khối "Nhận quản lý sân có sẵn" trong trang duyệt hồ sơ chủ sân. */
public record ExternalVenueClaimReviewResponse(
        ExternalVenueResponse venue,
        String region,
        Integer distanceMeters,
        boolean farAway,
        long wantCount,
        long matchCount,
        long otherPendingClaims
) {
    public static ExternalVenueClaimReviewResponse from(ExternalVenueClaimReview review) {
        return new ExternalVenueClaimReviewResponse(ExternalVenueResponse.from(review.venue()), review.venue().getRegion(),
                review.distanceMeters(), review.farAway(), review.wantCount(), review.matchCount(),
                review.otherPendingClaims());
    }
}
