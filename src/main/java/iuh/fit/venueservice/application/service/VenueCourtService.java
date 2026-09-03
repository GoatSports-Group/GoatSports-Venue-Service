package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.VenueCourt;

import java.util.List;
import java.util.UUID;

public interface VenueCourtService {
    VenueCourt createVenueCourt(CreateVenueCourtCommand command) throws ServiceException;

    List<VenueCourt> getVenueCourts(UUID venueId, UUID currentUserId) throws ServiceException;

    VenueCourt getVenueCourt(UUID venueCourtId, UUID currentUserId) throws ServiceException;

    VenueCourt updateVenueCourt(UpdateVenueCourtCommand command) throws ServiceException;

    VenueCourt updateActive(UUID venueCourtId, Boolean active, UUID currentUserId) throws ServiceException;

    void softDelete(UUID venueCourtId, UUID currentUserId) throws ServiceException;

    List<VenueCourt> getVenueCourts(UUID venueId) throws ServiceException;

    VenueCourt getVenueCourt(UUID venueCourtId) throws ServiceException;
}
