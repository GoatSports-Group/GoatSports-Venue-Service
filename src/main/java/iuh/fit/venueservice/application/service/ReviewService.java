package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CreatePlayerReviewCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.PublicReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewView;
import iuh.fit.venueservice.application.dto.response.venue.PlayerReviewView;
import iuh.fit.venueservice.application.dto.response.venue.PublicReviewView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.Review;
import iuh.fit.venueservice.domain.enums.ReviewStatus;

import java.util.UUID;

public interface ReviewService {
    PlayerReviewView createPlayerReview(CreatePlayerReviewCommand command) throws ServiceException;

    PageResult<OwnerReviewView> getOwnerReviews(OwnerReviewFilter filter) throws ServiceException;

    PageResult<PublicReviewView> getPublishedReviews(PublicReviewFilter filter) throws ServiceException;

    Review moderateReview(UUID reviewId, ReviewStatus targetStatus) throws ServiceException;
}
