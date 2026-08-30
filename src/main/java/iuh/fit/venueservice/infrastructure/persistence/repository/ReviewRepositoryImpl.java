package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewRecord;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.domain.enums.ReviewStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.ReviewEntity;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaReviewRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {
    private final JpaReviewRepository jpaRepository;
    private final JpaVenueRepository venueRepository;
    private final JpaBookingRepository bookingRepository;

    @Override
    public Review save(Review review) {
        ReviewEntity entity = review.getReviewId() == null
                ? new ReviewEntity()
                : jpaRepository.findById(review.getReviewId()).orElseGet(ReviewEntity::new);
        entity.setPlayerId(review.getPlayerId());
        entity.setRating(review.getRating());
        entity.setContent(review.getContent());
        entity.setStatus(review.getStatus());
        entity.setVenue(venueRepository.getReferenceById(review.getVenueId()));
        entity.setBooking(bookingRepository.getReferenceById(review.getBookingId()));
        return toDomain(jpaRepository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findByBookingId(UUID bookingId) {
        return jpaRepository.findByBooking_BookingId(bookingId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerReviewRecord> findOwnedReviews(OwnerReviewFilter filter) {
        LocalDateTime fromTime = filter.fromDate() == null
                ? null : filter.fromDate().atStartOfDay();
        LocalDateTime toTime = filter.toDate() == null
                ? null : filter.toDate().plusDays(1).atStartOfDay();
        var page = jpaRepository.findOwnedReviews(
                filter.ownerId(), filter.venueId(), filter.venueCourtId(), filter.rating(),
                fromTime, toTime, ReviewStatus.DELETED, PageRequest.of(filter.page(), filter.size())
        );
        return new PageResult<>(
                page.getContent().stream().map(this::toOwnerRecord).toList(),
                page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages()
        );
    }

    @Override
    public long countPublishedByVenueId(UUID venueId) {
        return jpaRepository.countByVenue_VenueIdAndStatus(venueId, ReviewStatus.PUBLISHED);
    }

    @Override
    public Double averagePublishedRatingByVenueId(UUID venueId) {
        return jpaRepository.averageRating(venueId, ReviewStatus.PUBLISHED);
    }

    private OwnerReviewRecord toOwnerRecord(ReviewEntity entity) {
        var booking = entity.getBooking();
        var court = booking.getVenueCourt();
        return new OwnerReviewRecord(
                toDomain(entity), entity.getVenue().getName(), court.getVenueCourtId(),
                court.getName(), booking.getBookingCode(), booking.getPlayDate(),
                booking.getStartTime(), booking.getEndTime()
        );
    }

    private Review toDomain(ReviewEntity entity) {
        return Review.builder()
                .reviewId(entity.getReviewId())
                .venueId(entity.getVenue().getVenueId())
                .playerId(entity.getPlayerId())
                .bookingId(entity.getBooking().getBookingId())
                .rating(entity.getRating())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
