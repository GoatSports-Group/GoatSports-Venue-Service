package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateCancellationPolicyCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.dto.request.file.MoveFileRequest;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.CancellationPolicyRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VenueServiceImpl implements VenueService {
    private final VenueRepository venueRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final StoragePort storagePort;

    public VenueServiceImpl(
            VenueRepository venueRepository,
            CancellationPolicyRepository cancellationPolicyRepository,
            StoragePort storagePort
    ) {
        this.venueRepository = venueRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.storagePort = storagePort;
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
    public List<Venue> getMyVenues(UUID currentUserId) {
        return venueRepository.findByUserId(currentUserId);
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
        command.validate();
        List<String> imageKeys = finalizeNewImages(command.imageUrls());

        venue.setName(command.name().trim());
        venue.setDescription(trimToNull(command.description()));
        venue.setOpenTime(command.openTime());
        venue.setCloseTime(command.closeTime());
        venue.setActive(command.active());
        venue.setMinPrice(command.minPrice());
        venue.setMaxPrice(command.maxPrice());
        venue.setPhone(command.phone().trim());
        venue.setEmail(command.email().trim());
        venue.setImageUrls(imageKeys);
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
    public Optional<VenueFacilityLayout> getFacilityLayout(UUID venueId, UUID currentUserId)
            throws ServiceException {
        return Optional.ofNullable(getOwnedVenue(venueId, currentUserId).getFacilityLayout());
    }

    @Override
    public VenueFacilityLayout updateFacilityLayout(
            UUID venueId,
            VenueFacilityLayout layout,
            UUID currentUserId
    ) throws ServiceException {
        Venue venue = getOwnedVenue(venueId, currentUserId);
        ensureValidFacilityLayout(venue, layout);
        VenueFacilityLayout savedLayout = new VenueFacilityLayout(
                1,
                venueId,
                List.copyOf(layout.items()),
                List.copyOf(layout.zones()),
                LocalDateTime.now()
        );
        venue.setFacilityLayout(savedLayout);
        venueRepository.save(venue);
        return savedLayout;
    }

    @Override
    public CancellationPolicy updateCancellationPolicy(UpdateCancellationPolicyCommand command)
            throws ServiceException {
        command.validate();
        getOwnedVenue(command.venueId(), command.currentUserId());
        CancellationPolicy policy = cancellationPolicyRepository.findByVenueId(command.venueId())
                .orElseGet(CancellationPolicy::new);
        policy.setFullRefundHoursBefore(command.fullRefundHoursBefore());
        policy.setPartialRefundHoursBefore(command.partialRefundHoursBefore());
        policy.setPartialRefundPercentage(command.partialRefundPercentage());
        policy.setNoRefundHoursBefore(command.noRefundHoursBefore());
        return cancellationPolicyRepository.save(command.venueId(), policy);
    }

    private void ensureValidFacilityLayout(Venue venue, VenueFacilityLayout layout) throws ServiceException {
        if (layout == null || layout.items() == null || layout.zones() == null) {
            throw new ServiceException("Dữ liệu bố cục cơ sở không hợp lệ.");
        }
        Set<String> zoneIds = new HashSet<>();
        for (VenueFacilityLayout.Zone zone : layout.zones()) {
            if (!zoneIds.add(zone.id())) {
                throw new ServiceException("Mỗi khu vực trong bố cục phải có mã riêng.");
            }
        }
        Set<String> itemIds = new HashSet<>();
        Set<UUID> ownedCourtIds = venue.getCourts().stream()
                .map(court -> court.getVenueCourtId())
                .collect(Collectors.toSet());
        for (VenueFacilityLayout.Item item : layout.items()) {
            if (!itemIds.add(item.id())) {
                throw new ServiceException("Mỗi đối tượng trong bố cục phải có mã riêng.");
            }
            if ("COURT".equals(item.type())
                    && (item.courtId() == null || !ownedCourtIds.contains(item.courtId()))) {
                throw new ServiceException("Bố cục chứa sân không thuộc cơ sở này.");
            }
            if (item.zoneId() != null && !item.zoneId().isBlank() && !zoneIds.contains(item.zoneId())) {
                throw new ServiceException("Đối tượng đang tham chiếu khu vực không tồn tại.");
            }
        }
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

    private List<String> finalizeNewImages(List<String> values) throws ServiceException {
        List<String> imageKeys = cleanList(values);
        List<String> tempKeys = imageKeys.stream()
                .filter(this::isTemporaryKey)
                .toList();
        if (tempKeys.isEmpty()) return imageKeys;

        List<FileMoveRequest> requests = tempKeys.stream()
                .map(FileMoveRequest::new)
                .toList();
        List<String> permanentKeys = storagePort.confirmFile(requests);
        if (permanentKeys == null || permanentKeys.size() != tempKeys.size()
                || permanentKeys.stream().anyMatch(key -> key == null || key.isBlank() || isTemporaryKey(key))) {
            throw new ServiceException("Xác nhận hình ảnh cơ sở thất bại.");
        }

        Map<String, String> replacements = new HashMap<>();
        for (int index = 0; index < tempKeys.size(); index++) {
            replacements.put(tempKeys.get(index), permanentKeys.get(index));
        }

        storagePort.moveFile(new MoveFileRequest(requests, "venue-upload:" + UUID.randomUUID()));
        return imageKeys.stream()
                .map(key -> replacements.getOrDefault(key, key))
                .toList();
    }

    private boolean isTemporaryKey(String key) {
        return key.startsWith("temp/") || key.contains("/temp/");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
