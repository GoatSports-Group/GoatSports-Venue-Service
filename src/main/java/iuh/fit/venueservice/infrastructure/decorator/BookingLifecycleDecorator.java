package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingLifecycleService;
import iuh.fit.venueservice.application.service.impl.BookingLifecycleServiceImpl;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class BookingLifecycleDecorator implements BookingLifecycleService {
    private final BookingLifecycleServiceImpl delegate;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public BookingStatus applyTimedState(UUID bookingId, BookingStatus target) throws ServiceException {
        return delegate.applyTimedState(bookingId, target);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findBookingsWithoutLifecycle(int limit) {
        return delegate.findBookingsWithoutLifecycle(limit);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void adoptIntoLifecycle(UUID bookingId) throws ServiceException {
        delegate.adoptIntoLifecycle(bookingId);
    }
}
