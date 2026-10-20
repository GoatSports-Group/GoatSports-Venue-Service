package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public interface BookingPaymentEventService {
    boolean applySucceededPayment(PaymentSucceededCommand command) throws ServiceException;

    boolean applyTerminalPayment(PaymentTerminalCommand command) throws ServiceException;

    /**
     * Admin nhắc khoản hoàn tiền mặt chủ sân chưa xác nhận: báo lại cho chủ sân của lượt đặt. Khoản đã xử lý xong
     * hoặc không thuộc lượt hủy sân nào thì bỏ qua (false).
     */
    boolean remindOwnerOfManualRefund(UUID refundId, UUID reminderId, double amount) throws ServiceException;
}
