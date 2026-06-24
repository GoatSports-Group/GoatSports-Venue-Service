package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.VenueOwnerProfileRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueOwnerProfile;

import java.util.List;

public class VenueServiceImpl implements VenueService {
    private final VenueOwnerProfileRepository venueOwnerProfileRepository;
    private final VenueRepository venueRepository;

    public VenueServiceImpl(
            VenueOwnerProfileRepository venueOwnerProfileRepository,
            VenueRepository venueRepository
    ) {
        this.venueOwnerProfileRepository = venueOwnerProfileRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public Venue createVenue(CreateVenueCommand command) throws ServiceException {
        VenueOwnerProfile profile = venueOwnerProfileRepository.findByUserId(command.currentUserId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy hồ sơ chủ sân. Bạn cần đăng ký làm chủ sân trước."));

        if (profile.getVerified() == null || !profile.getVerified()) {
            throw new ServiceException("Bạn hãy xác nhận hồ sơ của mình để tạo sân.");
        }

        List<Venue> existingVenues = venueRepository.findByUserId(command.currentUserId());
        if (!existingVenues.isEmpty()) {
            throw new ServiceException("Mỗi chủ sân chỉ được tạo tối đa một tổ hợp sân.");
        }

        Venue venue = Venue.builder()
                .name(command.name())
                .description(command.description())
                .openTime(command.openTime())
                .closeTime(command.closeTime())
                .active(true)
                .minPrice(command.minPrice())
                .maxPrice(command.maxPrice())
                .phone(command.phone())
                .email(command.email())
                .venueOwnerProfile(profile)
                .build();

        return venueRepository.save(venue);
    }
}
