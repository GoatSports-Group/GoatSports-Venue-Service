package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.ReviewEntity;
import iuh.fit.venueservice.domain.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    List<ReviewEntity> findByVenue_VenueId(UUID venueId);
    Optional<ReviewEntity> findByBooking_BookingId(UUID bookingId);

    @Query(value = """
            select review from ReviewEntity review
            join fetch review.venue venue
            join fetch review.booking booking
            join fetch booking.venueCourt court
            where venue.ownerId = :ownerId
              and review.status <> :excludedStatus
              and (:venueId is null or venue.venueId = :venueId)
              and (:venueCourtId is null or court.venueCourtId = :venueCourtId)
              and (:rating is null or review.rating = :rating)
              and (:fromTime is null or review.createdAt >= :fromTime)
              and (:toTime is null or review.createdAt < :toTime)
            order by review.createdAt desc, review.reviewId desc
            """, countQuery = """
            select count(review) from ReviewEntity review
            join review.venue venue
            join review.booking booking
            join booking.venueCourt court
            where venue.ownerId = :ownerId
              and review.status <> :excludedStatus
              and (:venueId is null or venue.venueId = :venueId)
              and (:venueCourtId is null or court.venueCourtId = :venueCourtId)
              and (:rating is null or review.rating = :rating)
              and (:fromTime is null or review.createdAt >= :fromTime)
              and (:toTime is null or review.createdAt < :toTime)
            """)
    Page<ReviewEntity> findOwnedReviews(
            @Param("ownerId") UUID ownerId,
            @Param("venueId") UUID venueId,
            @Param("venueCourtId") UUID venueCourtId,
            @Param("rating") Integer rating,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("excludedStatus") ReviewStatus excludedStatus,
            Pageable pageable
    );

    long countByVenue_VenueIdAndStatus(UUID venueId, ReviewStatus status);

    @Query("select avg(review.rating) from ReviewEntity review "
            + "where review.venue.venueId = :venueId and review.status = :status")
    Double averageRating(
            @Param("venueId") UUID venueId,
            @Param("status") ReviewStatus status
    );
}
