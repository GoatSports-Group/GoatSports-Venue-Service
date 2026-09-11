package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;
import iuh.fit.venueservice.presentation.converter.VenueSearchConverter;
import iuh.fit.venueservice.presentation.request.venue.UpdateVenueRequest;
import iuh.fit.venueservice.presentation.request.venue.UpdateVenueFacilityLayoutRequest;
import iuh.fit.venueservice.presentation.response.venue.VenueDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/venues")
@RequiredArgsConstructor
public class OwnerVenueController {
    private final VenueService venueService;
    private final CurrentUserProvider currentUserProvider;
    private final VenueSearchConverter venueSearchConverter;

    @GetMapping
    @PreAuthorize("hasAuthority('GET_OWN_VENUES')")
    public ResponseEntity<List<VenueDetailResponse>> getMyVenues() throws ServiceException {
        UUID currentUserId = currentUserId();
        List<VenueDetailResponse> response = venueService.getMyVenues(currentUserId)
                .stream()
                .map(venueSearchConverter::toDetailResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{venueId}")
    @PreAuthorize("hasAuthority('GET_OWN_VENUE')")
    public ResponseEntity<VenueDetailResponse> getOwnedVenue(@PathVariable UUID venueId)
            throws ServiceException {
        Venue venue = venueService.getOwnedVenue(venueId, currentUserId());
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(venue));
    }

    @PutMapping("/{venueId}")
    @PreAuthorize("hasAuthority('UPDATE_OWN_VENUE')")
    public ResponseEntity<VenueDetailResponse> updateVenue(
            @PathVariable UUID venueId,
            @Valid @RequestBody UpdateVenueRequest request
    ) throws ServiceException {
        UpdateVenueCommand command = new UpdateVenueCommand(
                venueId,
                request.getName(),
                request.getDescription(),
                request.getOpenTime(),
                request.getCloseTime(),
                request.getActive(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getPhone(),
                request.getEmail(),
                request.getAddress(),
                request.getWard(),
                request.getDistrict(),
                request.getCity(),
                request.getLatitude(),
                request.getLongitude(),
                request.getImageUrls(),
                request.getAmenities(),
                currentUserId()
        );
        Venue updated = venueService.updateVenue(command);
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(updated));
    }

    @GetMapping("/{venueId}/facility-layout")
    @PreAuthorize("hasAuthority('GET_OWN_VENUE')")
    public ResponseEntity<VenueFacilityLayout> getFacilityLayout(@PathVariable UUID venueId)
            throws ServiceException {
        return venueService.getFacilityLayout(venueId, currentUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{venueId}/facility-layout")
    @PreAuthorize("hasAuthority('UPDATE_OWN_VENUE')")
    public ResponseEntity<VenueFacilityLayout> updateFacilityLayout(
            @PathVariable UUID venueId,
            @Valid @RequestBody UpdateVenueFacilityLayoutRequest request
    ) throws ServiceException {
        VenueFacilityLayout layout = new VenueFacilityLayout(
                1,
                venueId,
                request.items().stream()
                        .map(item -> new VenueFacilityLayout.Item(
                                item.id(), item.type(), item.courtId(), item.label(), item.x(), item.y(),
                                item.width(), item.height(), item.rotation(), item.zoneId(), item.icon()
                        ))
                        .toList(),
                request.zones().stream()
                        .map(zone -> new VenueFacilityLayout.Zone(
                                zone.id(), zone.name(), zone.x(), zone.y(), zone.width(), zone.height()
                        ))
                        .toList(),
                null
        );
        return ResponseEntity.ok(venueService.updateFacilityLayout(venueId, layout, currentUserId()));
    }

    private UUID currentUserId() throws ServiceException {
        String value = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        return BaseUtil.uuid(value);
    }
}
