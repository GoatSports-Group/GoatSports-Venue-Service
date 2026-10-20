package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import iuh.fit.venueservice.application.service.impl.BookingPaymentEventServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class BookingPaymentEventDecorator implements BookingPaymentEventService {
    private final BookingPaymentEventServiceImpl delegate;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public boolean applySucceededPayment(PaymentSucceededCommand command) throws ServiceException {
        return delegate.applySucceededPayment(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public boolean applyTerminalPayment(PaymentTerminalCommand command) throws ServiceException {
        return delegate.applyTerminalPayment(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public boolean remindOwnerOfManualRefund(UUID refundId, UUID reminderId, double amount) throws ServiceException {
        return delegate.remindOwnerOfManualRefund(refundId, reminderId, amount);
    }
}
