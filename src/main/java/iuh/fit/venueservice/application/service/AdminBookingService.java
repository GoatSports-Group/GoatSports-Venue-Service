package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.AdminBookingFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

/** Admin xem đơn đặt sân của mọi cơ sở (chỉ đọc; thao tác trên đơn vẫn thuộc chủ sân). */
public interface AdminBookingService {
    PageResult<OwnerBookingView> getBookings(AdminBookingFilter filter) throws ServiceException;

    OwnerBookingView getBooking(UUID bookingId) throws ServiceException;
}
