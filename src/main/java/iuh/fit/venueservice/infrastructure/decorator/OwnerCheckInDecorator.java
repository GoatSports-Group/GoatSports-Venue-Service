package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.ConfirmOwnerCheckInCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateWalkInBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInLookupCommand;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCheckInView;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerCheckInService;
import iuh.fit.venueservice.application.service.impl.OwnerCheckInServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@RequiredArgsConstructor
public class OwnerCheckInDecorator implements OwnerCheckInService {
    private final OwnerCheckInServiceImpl delegate;

    @Override
    @Transactional(readOnly = true)
    public OwnerCheckInView lookup(OwnerCheckInLookupCommand command) throws ServiceException {
        return delegate.lookup(command);
    }

    @Override
    @Transactional
    public OwnerCheckInView confirm(ConfirmOwnerCheckInCommand command) throws ServiceException {
        return delegate.confirm(command);
    }

    @Override
    @Transactional
    public OwnerCheckInView createWalkIn(CreateWalkInBookingCommand command) throws ServiceException {
        return delegate.createWalkIn(command);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerCheckInView> history(OwnerCheckInFilter filter) throws ServiceException {
        return delegate.history(filter);
    }
}
