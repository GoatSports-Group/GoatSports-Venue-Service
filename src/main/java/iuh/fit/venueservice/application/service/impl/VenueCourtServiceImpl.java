package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.CourtAvailabilityStatus;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VenueCourtServiceImpl implements VenueCourtService {
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final Clock clock;

    public VenueCourtServiceImpl(
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotRepository timeSlotRepository,
            Clock clock
    ) {
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.clock = clock;
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

        return enrichAvailability(venueCourtRepository.save(court, venue));
    }

    @Override
    public List<VenueCourt> getVenueCourts(UUID venueId, UUID currentUserId) throws ServiceException {
        Venue venue = getOwnedVenue(venueId, currentUserId);
        return enrichAvailability(venueCourtRepository.findByVenueId(venue.getVenueId()));
    }

    @Override
    public VenueCourt getVenueCourt(UUID venueCourtId, UUID currentUserId) throws ServiceException {
        return enrichAvailability(getOwnedCourt(venueCourtId, currentUserId).court());
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
        return enrichAvailability(venueCourtRepository.save(court, owned.venue()));
    }

    @Override
    public VenueCourt updateActive(UUID venueCourtId, Boolean active, UUID currentUserId) throws ServiceException {
        OwnedCourt owned = getOwnedCourt(venueCourtId, currentUserId);
        owned.court().setActive(active);
        return enrichAvailability(venueCourtRepository.save(owned.court(), owned.venue()));
    }

    @Override
    public List<VenueCourt> getVenueCourts(UUID venueId) throws ServiceException {
        getVenue(venueId);
        return enrichAvailability(venueCourtRepository.findByVenueId(venueId));
    }

    @Override
    public VenueCourt getVenueCourt(UUID venueCourtId) throws ServiceException {
        VenueCourt court = venueCourtRepository.findById(venueCourtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân thi đấu."));
        return enrichAvailability(court);
    }

    private OwnedCourt getOwnedCourt(UUID venueCourtId, UUID currentUserId) throws ServiceException {
        VenueCourt court = venueCourtRepository.findById(venueCourtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân thi đấu."));
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

    private List<VenueCourt> enrichAvailability(List<VenueCourt> courts) {
        if (courts.isEmpty()) return courts;
        LocalDateTime now = LocalDateTime.now(clock);
        Set<UUID> courtIds = courts.stream().map(VenueCourt::getVenueCourtId).collect(Collectors.toSet());
        Map<UUID, TimeSlot> blockingSlots = timeSlotRepository
                .findBlockingSlotsAt(courtIds, now.toLocalDate(), now.toLocalTime())
                .stream()
                .collect(Collectors.toMap(
                        TimeSlot::getVenueCourtId,
                        Function.identity(),
                        this::higherPriority
                ));
        courts.forEach(court -> applyAvailability(court, blockingSlots.get(court.getVenueCourtId())));
        return courts;
    }

    private VenueCourt enrichAvailability(VenueCourt court) {
        return enrichAvailability(new ArrayList<>(List.of(court))).getFirst();
    }

    private void applyAvailability(VenueCourt court, TimeSlot blockingSlot) {
        if (!Boolean.TRUE.equals(court.getActive())) {
            court.setAvailabilityStatus(CourtAvailabilityStatus.INACTIVE);
            court.setUnavailableUntil(null);
            return;
        }
        if (blockingSlot == null) {
            court.setAvailabilityStatus(CourtAvailabilityStatus.AVAILABLE);
            court.setUnavailableUntil(null);
            return;
        }
        court.setAvailabilityStatus(switch (blockingSlot.getStatus()) {
            case LOCKED -> CourtAvailabilityStatus.HELD;
            case BOOKED -> CourtAvailabilityStatus.OCCUPIED;
            case MAINTENANCE -> CourtAvailabilityStatus.MAINTENANCE;
            case AVAILABLE -> CourtAvailabilityStatus.AVAILABLE;
        });
        court.setUnavailableUntil(blockingSlot.getEndTime());
    }

    private TimeSlot higherPriority(TimeSlot left, TimeSlot right) {
        return priority(left.getStatus()) >= priority(right.getStatus()) ? left : right;
    }

    private int priority(TimeSlotStatus status) {
        return switch (status) {
            case MAINTENANCE -> 3;
            case BOOKED -> 2;
            case LOCKED -> 1;
            case AVAILABLE -> 0;
        };
    }

    private record OwnedCourt(VenueCourt court, Venue venue) {}
}
