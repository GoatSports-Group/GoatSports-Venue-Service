package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.application.service.impl.VenueCourtServiceImpl;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VenueCourtDecorator implements VenueCourtService {
    private final VenueCourtServiceImpl venueCourtServiceImpl;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public VenueCourt createVenueCourt(CreateVenueCourtCommand command) throws ServiceException {
        return venueCourtServiceImpl.createVenueCourt(command);
    }
}
