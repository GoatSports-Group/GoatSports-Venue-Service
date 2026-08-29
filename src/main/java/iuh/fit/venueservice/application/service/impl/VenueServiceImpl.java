package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.Venue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<Venue> getMyVenue(UUID currentUserId) {
        return venueRepository.findByUserId(currentUserId).stream().findFirst();
    }

    @Override
    public Venue getOwnedVenue(UUID venueId, UUID currentUserId) throws ServiceException {
        Venue venue = getVenue(venueId);
        assertOwner(venue, currentUserId);
        return venue;
    }

    @Override
    public Venue updateVenue(UpdateVenueCommand command) throws ServiceException {
        Venue venue = getOwnedVenue(command.venueId(), command.currentUserId());
        validate(command);

        venue.setName(command.name().trim());
        venue.setDescription(trimToNull(command.description()));
        venue.setOpenTime(command.openTime());
        venue.setCloseTime(command.closeTime());
        venue.setActive(command.active());
        venue.setMinPrice(command.minPrice());
        venue.setMaxPrice(command.maxPrice());
        venue.setPhone(command.phone().trim());
        venue.setEmail(command.email().trim());
        venue.setImageUrls(cleanList(command.imageUrls()));
        venue.setAmenities(cleanList(command.amenities()));

        Address address = venue.getAddress();
        if (address == null) {
            address = Address.builder().build();
            venue.setAddress(address);
        }
        address.setAddress(command.address().trim());
        address.setWard(trimToNull(command.ward()));
        address.setDistrict(trimToNull(command.district()));
        address.setCity(command.city().trim());
        address.setLatitude(command.latitude());
        address.setLongitude(command.longitude());

        return venueRepository.save(venue);
    }

    @Override
    public PageResult<Venue> getVenues(int page, int size) {
        return venueRepository.findAll(page, size);
    }

    @Override
    public Venue getVenue(UUID venueId) throws ServiceException {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở thể thao."));
    }

    private void validate(UpdateVenueCommand command) throws ServiceException {
        if (!command.openTime().isBefore(command.closeTime())) {
            throw new ServiceException("Giờ đóng cửa phải sau giờ mở cửa.");
        }
        if (command.minPrice() > command.maxPrice()) {
            throw new ServiceException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }
    }

    private void assertOwner(Venue venue, UUID currentUserId) throws OwnershipException {
        if (venue.getOwnerId() == null || !venue.getOwnerId().equals(currentUserId)) {
            throw new OwnershipException("Bạn không có quyền quản lý cơ sở này.");
        }
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
