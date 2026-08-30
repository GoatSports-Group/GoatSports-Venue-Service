package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CreatePlayerReviewCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerReviewView;
import iuh.fit.venueservice.application.dto.response.venue.PlayerReviewView;
import iuh.fit.venueservice.application.exception.ServiceException;

public interface ReviewService {
    PlayerReviewView createPlayerReview(CreatePlayerReviewCommand command) throws ServiceException;

    PageResult<OwnerReviewView> getOwnerReviews(OwnerReviewFilter filter) throws ServiceException;
}
