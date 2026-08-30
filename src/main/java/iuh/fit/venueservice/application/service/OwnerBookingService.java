package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.request.venue.UpdateOwnerBookingStatusCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public interface OwnerBookingService {
    PageResult<OwnerBookingView> getBookings(OwnerBookingFilter filter) throws ServiceException;

    OwnerBookingView getBooking(UUID bookingId, UUID ownerId) throws ServiceException;

    OwnerBookingView updateStatus(UpdateOwnerBookingStatusCommand command) throws ServiceException;
}
