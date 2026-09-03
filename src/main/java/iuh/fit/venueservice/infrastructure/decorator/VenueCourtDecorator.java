package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.application.service.impl.VenueCourtServiceImpl;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class VenueCourtDecorator implements VenueCourtService {
    private final VenueCourtServiceImpl venueCourtServiceImpl;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public VenueCourt createVenueCourt(CreateVenueCourtCommand command) throws ServiceException {
        return venueCourtServiceImpl.createVenueCourt(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueCourt> getVenueCourts(UUID venueId, UUID currentUserId) throws ServiceException {
        return venueCourtServiceImpl.getVenueCourts(venueId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueCourt getVenueCourt(UUID venueCourtId, UUID currentUserId) throws ServiceException {
        return venueCourtServiceImpl.getVenueCourt(venueCourtId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public VenueCourt updateVenueCourt(UpdateVenueCourtCommand command) throws ServiceException {
        return venueCourtServiceImpl.updateVenueCourt(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public VenueCourt updateActive(UUID venueCourtId, Boolean active, UUID currentUserId)
            throws ServiceException {
        return venueCourtServiceImpl.updateActive(venueCourtId, active, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void softDelete(UUID venueCourtId, UUID currentUserId) throws ServiceException {
        venueCourtServiceImpl.softDelete(venueCourtId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueCourt> getVenueCourts(UUID venueId) throws ServiceException {
        return venueCourtServiceImpl.getVenueCourts(venueId);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueCourt getVenueCourt(UUID venueCourtId) throws ServiceException {
        return venueCourtServiceImpl.getVenueCourt(venueCourtId);
    }
}
