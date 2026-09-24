package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.BookingCancellation;

import java.util.UUID;

/**
 * Hủy đơn đặt sân và hoàn tiền cọc: người chơi yêu cầu, hệ thống tự duyệt (chưa trả tiền hoặc trong cửa sổ
 * tự duyệt sau khi đặt) hoặc chờ chủ sân duyệt; hoàn tiền đi qua payment-service.
 */
public interface BookingCancellationService {

    BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException;

    BookingCancellation processCancellation(UUID cancellationId, boolean approved, UUID processedByUserId,
                                            String processNote) throws ServiceException;

    /** Chủ sân xác nhận đã chuyển tiền tay cho khoản hoàn mà cổng thanh toán không tự hoàn được. */
    BookingCancellation completeManualRefund(UUID cancellationId, UUID ownerId, String providerRefundId)
            throws ServiceException;
}
