package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.AdminReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewRecord;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.domain.entity.AiVerdict;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.domain.enums.ReviewStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.ReviewEntity;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaReviewRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    public Optional<Review> findById(UUID reviewId) {
        return jpaRepository.findById(reviewId).map(this::toDomain);
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
        return findRecords(reviewsSpecification(
                filter.ownerId(), filter.venueId(), filter.venueCourtId(), filter.rating(), null, fromTime, toTime,
                false
        ), filter.page(), filter.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerReviewRecord> findAllReviews(AdminReviewFilter filter) {
        return findRecords(reviewsSpecification(
                null, filter.venueId(), null, filter.rating(), filter.status(), null, null, filter.aiFlagged()
        ), filter.page(), filter.size());
    }

    private PageResult<OwnerReviewRecord> findRecords(Specification<ReviewEntity> specification, int page, int size) {
        var result = jpaRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("reviewId")))
        );
        return new PageResult<>(
                result.getContent().stream().map(this::toOwnerRecord).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Review> findPublishedByVenueId(UUID venueId, int page, int size) {
        var result = jpaRepository.findByVenue_VenueIdAndStatus(
                venueId,
                ReviewStatus.PUBLISHED,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")))
        );
        return new PageResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages()
        );
    }

    /** ownerId null là toàn nền tảng (admin); status null là mọi trạng thái trừ DELETED. */
    private Specification<ReviewEntity> reviewsSpecification(
            UUID ownerId, UUID venueId, UUID venueCourtId, Integer rating, ReviewStatus status,
            LocalDateTime fromTime, LocalDateTime toTime, boolean aiFlagged
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (ownerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("venue").get("ownerId"), ownerId));
            }
            predicates.add(status == null
                    ? criteriaBuilder.notEqual(root.get("status"), ReviewStatus.DELETED)
                    : criteriaBuilder.equal(root.get("status"), status));

            if (venueId != null) {
                predicates.add(criteriaBuilder.equal(root.get("venue").get("venueId"), venueId));
            }
            if (venueCourtId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("booking").get("venueCourt").get("venueCourtId"), venueCourtId
                ));
            }
            if (rating != null) {
                predicates.add(criteriaBuilder.equal(root.get("rating"), rating));
            }
            if (fromTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromTime));
            }
            if (toTime != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), toTime));
            }
            if (aiFlagged) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("aiSeverity"), AiVerdict.FLAG_SEVERITY));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    @Override
    public long countPublishedByVenueId(UUID venueId) {
        return jpaRepository.countByVenue_VenueIdAndStatus(venueId, ReviewStatus.PUBLISHED);
    }

    @Override
    public Double averagePublishedRatingByVenueId(UUID venueId) {
        return jpaRepository.averageRating(venueId, ReviewStatus.PUBLISHED);
    }

    @Override
    public List<UUID> findAwaitingAiVerdict(int maxAttempts, int limit) {
        return jpaRepository.findAwaitingAiVerdict(maxAttempts, limit);
    }

    @Override
    @Transactional
    public void saveAiVerdict(UUID reviewId, AiVerdict verdict) {
        jpaRepository.saveAiVerdict(
                reviewId, verdict.label(), verdict.severity(), verdict.reason(), verdict.checkedAt());
    }

    @Override
    @Transactional
    public void addAiAttempts(UUID reviewId, int attempts) {
        jpaRepository.addAiAttempts(reviewId, attempts);
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
                .aiVerdict(entity.getAiCheckedAt() == null ? null : new AiVerdict(
                        entity.getAiLabel(), entity.getAiSeverity(), entity.getAiReason(), entity.getAiCheckedAt()))
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
