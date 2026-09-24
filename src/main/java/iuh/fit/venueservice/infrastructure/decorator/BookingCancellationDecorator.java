package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingCancellationService;
import iuh.fit.venueservice.application.service.impl.BookingCancellationServiceImpl;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Bọc use case hủy đơn trong transaction: tầng application không phụ thuộc Spring nên ranh giới giao dịch đặt ở đây. */
@Service
@Primary
@RequiredArgsConstructor
public class BookingCancellationDecorator implements BookingCancellationService {
    private final BookingCancellationServiceImpl delegate;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public BookingCancellation requestCancellation(CancelBookingCommand command) throws ServiceException {
        return delegate.requestCancellation(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public BookingCancellation processCancellation(UUID cancellationId, boolean approved, UUID processedByUserId,
                                                   String processNote) throws ServiceException {
        return delegate.processCancellation(cancellationId, approved, processedByUserId, processNote);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public BookingCancellation completeManualRefund(UUID cancellationId, UUID ownerId, String providerRefundId)
            throws ServiceException {
        return delegate.completeManualRefund(cancellationId, ownerId, providerRefundId);
    }
}
