package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.Venue;

import java.util.Optional;
import java.util.UUID;

public interface VenueService {
    Venue createVenue(CreateVenueCommand command) throws ServiceException;

    Optional<Venue> getMyVenue(UUID currentUserId);

    Venue getOwnedVenue(UUID venueId, UUID currentUserId) throws ServiceException;

    Venue updateVenue(UpdateVenueCommand command) throws ServiceException;

    PageResult<Venue> getVenues(int page, int size);

    Venue getVenue(UUID venueId) throws ServiceException;
}
