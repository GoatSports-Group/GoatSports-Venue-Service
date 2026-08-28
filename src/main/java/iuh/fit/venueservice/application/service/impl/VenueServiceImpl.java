package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.Venue;

import java.util.List;

public class VenueServiceImpl implements VenueService {
    private final VenueRepository venueRepository;

    public VenueServiceImpl(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Override
    public Venue createVenue(CreateVenueCommand command) throws ServiceException {
        List<Venue> existingVenues = venueRepository.findByUserId(command.currentUserId());
        if (!existingVenues.isEmpty()) {
            throw new ServiceException("Mỗi chủ sân chỉ được tạo tối đa một tổ hợp sân.");
        }

        Address address = Address.builder()
                .address(command.address())
                .ward(command.ward())
                .district(command.district())
                .city(command.city())
                .latitude(command.latitude())
                .longitude(command.longitude())
                .build();

        Venue venue = Venue.builder()
                .name(command.name())
                .description(command.description())
                .openTime(command.openTime())
                .closeTime(command.closeTime())
                .active(command.active())
                .minPrice(command.minPrice())
                .maxPrice(command.maxPrice())
                .phone(command.phone())
                .email(command.email())
                .imageUrls(command.imageUrls())
                .amenities(command.amenities())
                .ownerId(command.currentUserId())
                .address(address)
                .build();

        return venueRepository.save(venue);
    }
}
