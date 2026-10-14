package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.ReviewEntity;
import iuh.fit.venueservice.domain.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaReviewRepository extends JpaRepository<ReviewEntity, UUID>,
        JpaSpecificationExecutor<ReviewEntity> {
    List<ReviewEntity> findByVenue_VenueId(UUID venueId);
    Page<ReviewEntity> findByVenue_VenueIdAndStatus(UUID venueId, ReviewStatus status, Pageable pageable);
    Optional<ReviewEntity> findByBooking_BookingId(UUID bookingId);

    @Override
    @EntityGraph(attributePaths = {"venue", "venue.cancellationPolicy", "booking", "booking.venueCourt",
            "booking.cancellation", "booking.checkInRecord"})
    Page<ReviewEntity> findAll(Specification<ReviewEntity> specification, Pageable pageable);

    long countByVenue_VenueIdAndStatus(UUID venueId, ReviewStatus status);

    @Query("select avg(review.rating) from ReviewEntity review "
            + "where review.venue.venueId = :venueId and review.status = :status")
    Double averageRating(
            @Param("venueId") UUID venueId,
            @Param("status") ReviewStatus status
    );

    /** Đánh giá đang hiển thị có chữ, AI chưa đọc và chưa hết lượt thử; mới trước. */
    @Query(value = """
            select review_id from reviews
            where status = 'PUBLISHED' and content is not null and btrim(content) <> ''
              and ai_checked_at is null and ai_attempts < :maxAttempts
            order by created_at desc
            limit :limit""", nativeQuery = true)
    List<UUID> findAwaitingAiVerdict(@Param("maxAttempts") int maxAttempts, @Param("limit") int limit);

    @Modifying
    @Query(value = """
            update reviews set ai_label = :label, ai_severity = :severity, ai_reason = :reason,
              ai_checked_at = :checkedAt
            where review_id = :reviewId""", nativeQuery = true)
    void saveAiVerdict(
            @Param("reviewId") UUID reviewId,
            @Param("label") String label,
            @Param("severity") Integer severity,
            @Param("reason") String reason,
            @Param("checkedAt") LocalDateTime checkedAt
    );

    @Modifying
    @Query(value = "update reviews set ai_attempts = ai_attempts + :attempts where review_id = :reviewId",
            nativeQuery = true)
    void addAiAttempts(@Param("reviewId") UUID reviewId, @Param("attempts") int attempts);
}
