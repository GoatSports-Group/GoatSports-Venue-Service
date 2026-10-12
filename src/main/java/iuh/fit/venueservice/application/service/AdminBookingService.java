package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.AdminBookingFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

/** Admin xem đơn đặt sân của mọi cơ sở (chỉ đọc; thao tác trên đơn vẫn thuộc chủ sân). */
public interface AdminBookingService {
    PageResult<OwnerBookingView> getBookings(AdminBookingFilter filter) throws ServiceException;

    OwnerBookingView getBooking(UUID bookingId) throws ServiceException;

    /**
     * Mọi đơn khớp bộ lọc (bỏ qua phân trang), cho report-service dựng báo cáo trong một lần gọi.
     * Quá {@code maxRows} đơn thì từ chối để không dựng file quá lớn.
     */
    List<OwnerBookingView> exportBookings(AdminBookingFilter filter, int maxRows) throws ServiceException;
}
