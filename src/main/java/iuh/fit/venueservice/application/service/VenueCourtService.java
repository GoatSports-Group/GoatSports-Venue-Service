package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.VenueCourt;

public interface VenueCourtService {
    VenueCourt createVenueCourt(CreateVenueCourtCommand command) throws ServiceException;
}
