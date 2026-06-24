package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.Venue;

public interface VenueService {
    Venue createVenue(CreateVenueCommand command) throws ServiceException;
}
