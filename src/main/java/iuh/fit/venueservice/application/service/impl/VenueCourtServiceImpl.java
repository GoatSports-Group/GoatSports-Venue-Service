package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.SportType;

public class VenueCourtServiceImpl implements VenueCourtService {
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;

    public VenueCourtServiceImpl(
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public VenueCourt createVenueCourt(CreateVenueCourtCommand command) throws ServiceException {
        Venue venue = venueRepository.findById(command.venueId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy sân."));

        if (venue.getVenueOwnerProfile() == null || !venue.getVenueOwnerProfile().getUserId().equals(command.currentUserId()))
        {
            throw new ServiceException("Bạn không có quyền quản lý sân này.");
        }

        VenueCourt court = VenueCourt.builder()
                .sportType(SportType.valueOf(command.sportType()))
                .name(command.name())
                .capacity(command.capacity())
                .pricePerHour(command.pricePerHour())
                .indoor(command.indoor())
                .active(true)
                .build();

        return venueCourtRepository.save(court, venue);
    }
}
