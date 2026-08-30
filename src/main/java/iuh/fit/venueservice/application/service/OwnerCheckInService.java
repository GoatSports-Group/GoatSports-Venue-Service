package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.ConfirmOwnerCheckInCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateWalkInBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInLookupCommand;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCheckInView;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;

public interface OwnerCheckInService {
    OwnerCheckInView lookup(OwnerCheckInLookupCommand command) throws ServiceException;

    OwnerCheckInView confirm(ConfirmOwnerCheckInCommand command) throws ServiceException;

    OwnerCheckInView createWalkIn(CreateWalkInBookingCommand command) throws ServiceException;

    PageResult<OwnerCheckInView> history(OwnerCheckInFilter filter) throws ServiceException;
}
