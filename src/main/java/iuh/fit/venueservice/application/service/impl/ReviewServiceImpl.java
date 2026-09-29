package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.AdminReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.CreatePlayerReviewCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.PublicReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewView;
import iuh.fit.venueservice.application.dto.response.venue.PlayerReviewView;
import iuh.fit.venueservice.application.dto.response.venue.PublicReviewView;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.ReviewService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.ReviewStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    @Transactional
    public PlayerReviewView createPlayerReview(CreatePlayerReviewCommand command) throws ServiceException {
        command.validate();
        Booking booking = bookingRepository.findByIdForUpdate(command.bookingId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy booking."));
        if (booking.getPlayerId() == null || !booking.getPlayerId().equals(command.playerId())) {
            throw new OwnershipException("Booking không thuộc tài khoản của bạn.");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ServiceException("Chỉ booking đã hoàn tất mới được đánh giá.");
        }
        if (reviewRepository.findByBookingId(command.bookingId()).isPresent()) {
            throw new ServiceException("Booking này đã được đánh giá.");
        }

        VenueCourt court = venueCourtRepository.findById(booking.getVenueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân của booking."));
        Venue venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở của booking."));
        Review saved = reviewRepository.save(Review.builder()
                .venueId(venue.getVenueId())
                .playerId(command.playerId())
                .bookingId(command.bookingId())
                .rating(command.rating())
                .content(normalizeContent(command.content()))
                .status(ReviewStatus.PUBLISHED)
                .build());
        refreshVenueRating(venue);
        return toPlayerView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerReviewView> getOwnerReviews(OwnerReviewFilter filter) throws ServiceException {
        filter.validate();
        ensureRequestedScope(filter);
        PageResult<OwnerReviewRecord> page = reviewRepository.findOwnedReviews(filter);
        return new PageResult<>(
                page.items().stream().map(this::toOwnerView).toList(),
                page.total(), page.page(), page.pageSize(), page.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerReviewView> getAllReviews(AdminReviewFilter filter) throws ServiceException {
        filter.validate();
        PageResult<OwnerReviewRecord> page = reviewRepository.findAllReviews(filter);
        return new PageResult<>(
                page.items().stream().map(this::toOwnerView).toList(),
                page.total(), page.page(), page.pageSize(), page.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PublicReviewView> getPublishedReviews(PublicReviewFilter filter)
            throws ServiceException {
        filter.validate();
        venueRepository.findById(filter.venueId())
                .filter(venue -> Boolean.TRUE.equals(venue.getActive()))
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở đang hoạt động."));
        PageResult<Review> page = reviewRepository.findPublishedByVenueId(
                filter.venueId(), filter.page(), filter.size()
        );
        return new PageResult<>(
                page.items().stream().map(this::toPublicView).toList(),
                page.total(), page.page(), page.pageSize(), page.totalPages()
        );
    }

    private void ensureRequestedScope(OwnerReviewFilter filter) throws ServiceException {
        Venue selectedVenue = null;
        if (filter.venueId() != null) {
            selectedVenue = getOwnedVenue(filter.venueId(), filter.ownerId());
        }
        if (filter.venueCourtId() == null) return;
        VenueCourt court = venueCourtRepository.findById(filter.venueCourtId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân thi đấu."));
        Venue courtVenue = getOwnedVenue(court.getVenueId(), filter.ownerId());
        if (selectedVenue != null && !selectedVenue.getVenueId().equals(courtVenue.getVenueId())) {
            throw new ServiceException("Sân thi đấu không thuộc cơ sở đã chọn.");
        }
    }

    private Venue getOwnedVenue(UUID venueId, UUID ownerId) throws ServiceException {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở."));
        if (venue.getOwnerId() == null || !venue.getOwnerId().equals(ownerId)) {
            throw new OwnershipException("Cơ sở không thuộc quyền sở hữu của bạn.");
        }
        return venue;
    }

    @Override
    @Transactional
    public Review moderateReview(UUID reviewId, ReviewStatus targetStatus) throws ServiceException {
        if (targetStatus == null
                || (targetStatus != ReviewStatus.PUBLISHED
                && targetStatus != ReviewStatus.HIDDEN
                && targetStatus != ReviewStatus.REMOVED)) {
            throw new ServiceException("Trạng thái kiểm duyệt không hợp lệ.");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy đánh giá."));
        if (review.getStatus() == ReviewStatus.REMOVED) {
            throw new ServiceException("Đánh giá đã bị gỡ và không thể đổi trạng thái.");
        }
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ServiceException("Đánh giá đã bị xoá.");
        }
        if (review.getStatus() == targetStatus) {
            return review;
        }

        review.setStatus(targetStatus);
        Review saved = reviewRepository.save(review);

        Venue venue = venueRepository.findById(review.getVenueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở của đánh giá."));
        refreshVenueRating(venue);
        return saved;
    }

    private void refreshVenueRating(Venue venue) {
        long count = reviewRepository.countPublishedByVenueId(venue.getVenueId());
        Double average = reviewRepository.averagePublishedRatingByVenueId(venue.getVenueId());
        venue.setTotalReviews(Math.toIntExact(count));
        venue.setAverageRating(average == null ? 0.0 : average);
        venueRepository.save(venue);
    }

    private String normalizeContent(String content) {
        if (content == null || content.trim().isEmpty()) return null;
        return content.trim();
    }

    private PlayerReviewView toPlayerView(Review review) {
        return new PlayerReviewView(
                review.getReviewId(), review.getVenueId(), review.getBookingId(),
                review.getRating(), review.getContent(), review.getStatus(), review.getCreatedAt()
        );
    }

    private PublicReviewView toPublicView(Review review) {
        return new PublicReviewView(
                review.getReviewId(), review.getRating(), review.getContent(), review.getCreatedAt()
        );
    }

    private OwnerReviewView toOwnerView(OwnerReviewRecord record) {
        Review review = record.review();
        return new OwnerReviewView(
                review.getReviewId(), review.getVenueId(), record.venueName(),
                record.venueCourtId(), record.courtName(), review.getBookingId(),
                record.bookingCode(), record.playDate(), record.startTime(), record.endTime(),
                review.getRating(), review.getContent(), review.getStatus(), review.getCreatedAt()
        );
    }
}
