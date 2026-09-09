package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.dto.request.payment.RefundResultCommand;
import iuh.fit.venueservice.application.exception.ServiceException;

public interface BookingPaymentEventService {
    boolean applySucceededPayment(PaymentSucceededCommand command) throws ServiceException;

    boolean applyTerminalPayment(PaymentTerminalCommand command) throws ServiceException;

    boolean applyRefundResult(RefundResultCommand command) throws ServiceException;
}
