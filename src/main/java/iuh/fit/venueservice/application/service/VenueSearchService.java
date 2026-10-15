package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.VenueSearchResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;

import java.util.List;
import java.util.UUID;

public interface VenueSearchService {
    PageResult<VenueSearchResponseCommand> searchVenues(SearchVenueCommand command);

    Venue getVenueDetails(UUID venueId) throws ServiceException;

    /** Như {@link #getVenueDetails} và ghi một lượt xem cho gợi ý cá nhân hóa khi người xem đã đăng nhập. */
    Venue viewVenueDetails(UUID venueId, UUID viewerId) throws ServiceException;

    List<VenueCourt> getVenueCourts(UUID venueId) throws ServiceException;

    VenueCourt getVenueCourt(UUID venueCourtId) throws ServiceException;
}
