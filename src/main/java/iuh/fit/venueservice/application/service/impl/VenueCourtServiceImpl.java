package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.SportType;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

        assertOwner(venue, command.currentUserId());

        VenueCourt court = VenueCourt.builder()
                .sportType(parseSportType(command.sportType()))
                .name(command.name().trim())
                .capacity(command.capacity())
                .surfaceType(trimToNull(command.surfaceType()))
                .active(command.active())
                .build();

        return venueCourtRepository.save(court, venue);
    }

    @Override
    public List<VenueCourt> getVenueCourts(UUID venueId, UUID currentUserId) throws ServiceException {
        Venue venue = getOwnedVenue(venueId, currentUserId);
        return venueCourtRepository.findByVenueId(venue.getVenueId());
    }

    @Override
    public VenueCourt getVenueCourt(UUID venueCourtId, UUID currentUserId) throws ServiceException {
        return getOwnedCourt(venueCourtId, currentUserId).court();
    }

    @Override
    public VenueCourt updateVenueCourt(UpdateVenueCourtCommand command) throws ServiceException {
        OwnedCourt owned = getOwnedCourt(command.venueCourtId(), command.currentUserId());
        VenueCourt court = owned.court();
        court.setName(command.name().trim());
        court.setSportType(parseSportType(command.sportType()));
        court.setCapacity(command.capacity());
        court.setSurfaceType(trimToNull(command.surfaceType()));
        court.setActive(command.active());
        return venueCourtRepository.save(court, owned.venue());
    }

    @Override
    public VenueCourt updateActive(UUID venueCourtId, Boolean active, UUID currentUserId) throws ServiceException {
        OwnedCourt owned = getOwnedCourt(venueCourtId, currentUserId);
        owned.court().setActive(active);
        return venueCourtRepository.save(owned.court(), owned.venue());
    }

    @Override
    public List<VenueCourt> getVenueCourts(UUID venueId) throws ServiceException {
        getVenue(venueId);
        return venueCourtRepository.findByVenueId(venueId);
    }

    @Override
    public VenueCourt getVenueCourt(UUID venueCourtId) throws ServiceException {
        return venueCourtRepository.findById(venueCourtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân thi đấu."));
    }

    private OwnedCourt getOwnedCourt(UUID venueCourtId, UUID currentUserId) throws ServiceException {
        VenueCourt court = getVenueCourt(venueCourtId);
        Venue venue = getOwnedVenue(court.getVenueId(), currentUserId);
        return new OwnedCourt(court, venue);
    }

    private Venue getOwnedVenue(UUID venueId, UUID currentUserId) throws ServiceException {
        Venue venue = getVenue(venueId);
        assertOwner(venue, currentUserId);
        return venue;
    }

    private Venue getVenue(UUID venueId) throws ResourceNotFoundException {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở thể thao."));
    }

    private void assertOwner(Venue venue, UUID currentUserId) throws OwnershipException {
        if (venue.getOwnerId() == null || !venue.getOwnerId().equals(currentUserId)) {
            throw new OwnershipException("Bạn không có quyền quản lý cơ sở này.");
        }
    }

    private SportType parseSportType(String value) throws ServiceException {
        try {
            return SportType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ServiceException("Môn thể thao không hợp lệ.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record OwnedCourt(VenueCourt court, Venue venue) {}
}
