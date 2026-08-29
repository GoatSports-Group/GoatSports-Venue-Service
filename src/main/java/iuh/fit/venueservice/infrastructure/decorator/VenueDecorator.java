package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.application.service.impl.VenueServiceImpl;
import iuh.fit.venueservice.domain.entity.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@RequiredArgsConstructor
public class VenueDecorator implements VenueService {
    private final VenueServiceImpl venueServiceImpl;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public Venue createVenue(CreateVenueCommand command) throws ServiceException {
        return venueServiceImpl.createVenue(command);
    }
}
