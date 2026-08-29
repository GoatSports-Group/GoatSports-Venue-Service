package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.application.service.impl.VenueServiceImpl;
import iuh.fit.venueservice.domain.entity.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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

    @Override
    @Transactional(readOnly = true)
    public Optional<Venue> getMyVenue(UUID currentUserId) {
        return venueServiceImpl.getMyVenue(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Venue getOwnedVenue(UUID venueId, UUID currentUserId) throws ServiceException {
        return venueServiceImpl.getOwnedVenue(venueId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public Venue updateVenue(UpdateVenueCommand command) throws ServiceException {
        return venueServiceImpl.updateVenue(command);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Venue> getVenues(int page, int size) {
        return venueServiceImpl.getVenues(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Venue getVenue(UUID venueId) throws ServiceException {
        return venueServiceImpl.getVenue(venueId);
    }
}
