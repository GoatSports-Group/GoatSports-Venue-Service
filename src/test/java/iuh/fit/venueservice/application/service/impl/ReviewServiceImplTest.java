package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreatePlayerReviewCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.PublicReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewRecord;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VenueCourtRepository venueCourtRepository;
    @Mock private VenueRepository venueRepository;

    private ReviewServiceImpl service;
    private UUID playerId;
    private UUID ownerId;
    private UUID venueId;
    private UUID courtId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(
                reviewRepository, bookingRepository, venueCourtRepository, venueRepository
        );
        playerId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        venueId = UUID.randomUUID();
        courtId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @Test
    void createsOnePublishedReviewForCompletedOwnedBookingAndRefreshesAggregate() throws Exception {
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking()));
        when(reviewRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(court()));
        Venue venue = venue(ownerId);
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(reviewRepository.save(any())).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setReviewId(UUID.randomUUID());
            review.setCreatedAt(LocalDateTime.of(2026, 8, 30, 10, 0));
            return review;
        });
        when(reviewRepository.countPublishedByVenueId(venueId)).thenReturn(2L);
        when(reviewRepository.averagePublishedRatingByVenueId(venueId)).thenReturn(4.5);

        var result = service.createPlayerReview(new CreatePlayerReviewCommand(
                playerId, bookingId, 5, "  Sân tốt  "
        ));

        assertThat(result.status()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(result.content()).isEqualTo("Sân tốt");
        assertThat(venue.getTotalReviews()).isEqualTo(2);
        assertThat(venue.getAverageRating()).isEqualTo(4.5);
        verify(venueRepository).save(venue);
    }

    @Test
    void rejectsCrossPlayerBookingBeforeReadingOrWritingReview() {
        Booking booking = booking();
        booking.setPlayerId(UUID.randomUUID());
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.createPlayerReview(command()))
                .isInstanceOf(OwnershipException.class);
        verify(reviewRepository, never()).findByBookingId(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsIncompleteOrAlreadyReviewedBooking() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.CHECKED_IN);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        assertThatThrownBy(() -> service.createPlayerReview(command()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("hoàn tất");

        booking.setStatus(BookingStatus.COMPLETED);
        when(reviewRepository.findByBookingId(bookingId)).thenReturn(Optional.of(review()));
        assertThatThrownBy(() -> service.createPlayerReview(command()))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đã được đánh giá");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidRatingAndContentBeforeLockingBooking() {
        assertThatThrownBy(() -> service.createPlayerReview(new CreatePlayerReviewCommand(
                playerId, bookingId, 0, "x"
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("1 đến 5");
        assertThatThrownBy(() -> service.createPlayerReview(new CreatePlayerReviewCommand(
                playerId, bookingId, 5, "x".repeat(2001)
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("2000");
        verify(bookingRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void returnsOwnerScopedReviewWithoutPlayerIdentity() throws Exception {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue(ownerId)));
        OwnerReviewRecord record = new OwnerReviewRecord(
                review(), "Goat Arena", courtId, "Court 1", "GS123456",
                LocalDate.of(2026, 8, 20), LocalTime.of(18, 0), LocalTime.of(19, 0)
        );
        when(reviewRepository.findOwnedReviews(any())).thenReturn(
                new PageResult<>(List.of(record), 1, 0, 20, 1)
        );

        var page = service.getOwnerReviews(filter(venueId, null));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(view -> {
            assertThat(view.venueName()).isEqualTo("Goat Arena");
            assertThat(view.courtName()).isEqualTo("Court 1");
            assertThat(view.bookingCode()).isEqualTo("GS123456");
            assertThat(view.rating()).isEqualTo(5);
        });
    }

    @Test
    void rejectsCrossOwnerVenueAndMismatchedCourtScope() {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue(UUID.randomUUID())));
        assertThatThrownBy(() -> service.getOwnerReviews(filter(venueId, null)))
                .isInstanceOf(OwnershipException.class);
        verify(reviewRepository, never()).findOwnedReviews(any());

        UUID otherVenue = UUID.randomUUID();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue(ownerId)));
        when(venueCourtRepository.findById(courtId)).thenReturn(Optional.of(
                VenueCourt.builder().venueCourtId(courtId).venueId(otherVenue).build()
        ));
        when(venueRepository.findById(otherVenue)).thenReturn(Optional.of(
                Venue.builder().venueId(otherVenue).ownerId(ownerId).build()
        ));
        assertThatThrownBy(() -> service.getOwnerReviews(filter(venueId, courtId)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("cơ sở đã chọn");
    }

    @Test
    void validatesOwnerFilters() {
        assertThatThrownBy(() -> service.getOwnerReviews(new OwnerReviewFilter(
                ownerId, null, null, 6, null, null, 0, 20
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("1 đến 5");
        assertThatThrownBy(() -> service.getOwnerReviews(new OwnerReviewFilter(
                ownerId, null, null, null,
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1), 0, 20
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("không hợp lệ");
        assertThatThrownBy(() -> service.getOwnerReviews(new OwnerReviewFilter(
                ownerId, null, null, null, null, null, 0, 21
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("1 đến 20");
    }

    @Test
    void returnsPublishedReviewsForAnActiveVenueWithoutPlayerIdentity() throws Exception {
        Venue activeVenue = venue(ownerId);
        activeVenue.setActive(true);
        Review publishedReview = review();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(activeVenue));
        when(reviewRepository.findPublishedByVenueId(venueId, 0, 10)).thenReturn(
                new PageResult<>(List.of(publishedReview), 1, 0, 10, 1)
        );

        var result = service.getPublishedReviews(new PublicReviewFilter(venueId, 0, 10));

        assertThat(result.items()).singleElement().satisfies(view -> {
            assertThat(view.reviewId()).isEqualTo(publishedReview.getReviewId());
            assertThat(view.rating()).isEqualTo(5);
            assertThat(view.content()).isEqualTo("Tốt");
            assertThat(view.createdAt()).isEqualTo(publishedReview.getCreatedAt());
        });
    }

    @Test
    void rejectsPublicReviewsForInactiveVenueAndInvalidPagination() {
        assertThatThrownBy(() -> service.getPublishedReviews(new PublicReviewFilter(venueId, -1, 10)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("phân trang");

        Venue inactiveVenue = venue(ownerId);
        inactiveVenue.setActive(false);
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(inactiveVenue));
        assertThatThrownBy(() -> service.getPublishedReviews(new PublicReviewFilter(venueId, 0, 10)))
                .isInstanceOf(ServiceException.class).hasMessageContaining("đang hoạt động");
        verify(reviewRepository, never()).findPublishedByVenueId(venueId, 0, 10);
    }

    private CreatePlayerReviewCommand command() {
        return new CreatePlayerReviewCommand(playerId, bookingId, 5, "Tốt");
    }

    private OwnerReviewFilter filter(UUID selectedVenue, UUID selectedCourt) {
        return new OwnerReviewFilter(
                ownerId, selectedVenue, selectedCourt, null,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 30), 0, 20
        );
    }

    private Booking booking() {
        return Booking.builder().bookingId(bookingId).playerId(playerId)
                .venueCourtId(courtId).status(BookingStatus.COMPLETED).build();
    }

    private VenueCourt court() {
        return VenueCourt.builder().venueCourtId(courtId).venueId(venueId).build();
    }

    private Venue venue(UUID venueOwner) {
        return Venue.builder().venueId(venueId).ownerId(venueOwner).build();
    }

    private Review review() {
        return Review.builder().reviewId(UUID.randomUUID()).venueId(venueId)
                .playerId(playerId).bookingId(bookingId).rating(5).content("Tốt")
                .status(ReviewStatus.PUBLISHED)
                .createdAt(LocalDateTime.of(2026, 8, 30, 10, 0)).build();
    }

    private Review reviewWithStatus(UUID reviewId, ReviewStatus status) {
        return Review.builder()
                .reviewId(reviewId)
                .venueId(venueId)
                .playerId(playerId)
                .bookingId(bookingId)
                .rating(5)
                .content("Sân đẹp")
                .status(status)
                .build();
    }

    @Test
    void hidingReviewRecomputesVenueRatingBecauseOnlyPublishedCounts() throws Exception {
        UUID reviewId = UUID.randomUUID();
        Venue venue = Venue.builder().venueId(venueId).ownerId(ownerId).averageRating(5.0).totalReviews(1).build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(reviewWithStatus(reviewId, ReviewStatus.PUBLISHED)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(reviewRepository.countPublishedByVenueId(venueId)).thenReturn(0L);
        when(reviewRepository.averagePublishedRatingByVenueId(venueId)).thenReturn(null);

        Review result = service.moderateReview(reviewId, ReviewStatus.HIDDEN);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(venue.getAverageRating()).isEqualTo(0.0);
        assertThat(venue.getTotalReviews()).isEqualTo(0);
        verify(venueRepository).save(venue);
    }

    @Test
    void removedReviewCannotBeMovedBackToPublished() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(reviewWithStatus(reviewId, ReviewStatus.REMOVED)));

        assertThatThrownBy(() -> service.moderateReview(reviewId, ReviewStatus.PUBLISHED))
                .isInstanceOf(ServiceException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void moderatingToTheSameStatusDoesNotTouchTheDatabase() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(reviewWithStatus(reviewId, ReviewStatus.PUBLISHED)));

        service.moderateReview(reviewId, ReviewStatus.PUBLISHED);

        verify(reviewRepository, never()).save(any(Review.class));
        verify(venueRepository, never()).save(any(Venue.class));
    }

    @Test
    void deletedIsNotAModerationTarget() {
        UUID reviewId = UUID.randomUUID();

        assertThatThrownBy(() -> service.moderateReview(reviewId, ReviewStatus.DELETED))
                .isInstanceOf(ServiceException.class);
        verify(reviewRepository, never()).findById(any(UUID.class));
    }
}
