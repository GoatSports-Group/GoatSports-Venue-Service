package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.request.venue.UpdateOwnerBookingStatusCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateOwnerBookingPaymentCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingPaymentResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerBookingService;
import iuh.fit.venueservice.application.service.impl.OwnerBookingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class OwnerBookingDecorator implements OwnerBookingService {
    private final OwnerBookingServiceImpl delegate;

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerBookingView> getBookings(OwnerBookingFilter filter) throws ServiceException {
        return delegate.getBookings(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerBookingView getBooking(UUID bookingId, UUID ownerId) throws ServiceException {
        return delegate.getBooking(bookingId, ownerId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerBookingView updateStatus(UpdateOwnerBookingStatusCommand command) throws ServiceException {
        return delegate.updateStatus(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public OwnerBookingPaymentResult createPayment(CreateOwnerBookingPaymentCommand command)
            throws ServiceException {
        return delegate.createPayment(command);
    }
}
