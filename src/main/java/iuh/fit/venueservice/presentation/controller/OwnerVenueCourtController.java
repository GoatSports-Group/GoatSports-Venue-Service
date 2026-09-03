package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.CreateVenueCourtCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpdateVenueCourtCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.VenueCourtMapper;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.presentation.converter.VenueCourtConverter;
import iuh.fit.venueservice.presentation.request.venue.UpdateVenueCourtActiveRequest;
import iuh.fit.venueservice.presentation.request.venue.UpdateVenueCourtRequest;
import iuh.fit.venueservice.presentation.response.venue.VenueCourtResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class OwnerVenueCourtController {
    private final VenueCourtService venueCourtService;
    private final CurrentUserProvider currentUserProvider;
    private final VenueCourtMapper venueCourtMapper;
    private final VenueCourtConverter venueCourtConverter;

    @GetMapping("/venues/{venueId}/courts")
    @PreAuthorize("hasAuthority('GET_OWN_VENUE_COURTS')")
    public ResponseEntity<List<VenueCourtResponse>> getVenueCourts(@PathVariable UUID venueId)
            throws ServiceException {
        List<VenueCourtResponse> response = venueCourtService.getVenueCourts(venueId, currentUserId())
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/venues/{venueId}/courts")
    @PreAuthorize("hasAuthority('CREATE_OWN_VENUE_COURT')")
    public ResponseEntity<VenueCourtResponse> createVenueCourt(
            @PathVariable UUID venueId,
            @Valid @RequestBody UpdateVenueCourtRequest request
    ) throws ServiceException {
        CreateVenueCourtCommand command = new CreateVenueCourtCommand(
                venueId,
                request.getSportType(),
                request.getName(),
                request.getCapacity(),
                request.getSurfaceType(),
                request.getActive(),
                currentUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(venueCourtService.createVenueCourt(command)));
    }

    @GetMapping("/venue-courts/{venueCourtId}")
    @PreAuthorize("hasAuthority('GET_OWN_VENUE_COURT')")
    public ResponseEntity<VenueCourtResponse> getVenueCourt(@PathVariable UUID venueCourtId)
            throws ServiceException {
        return ResponseEntity.ok(toResponse(venueCourtService.getVenueCourt(venueCourtId, currentUserId())));
    }

    @PutMapping("/venue-courts/{venueCourtId}")
    @PreAuthorize("hasAuthority('UPDATE_OWN_VENUE_COURT')")
    public ResponseEntity<VenueCourtResponse> updateVenueCourt(
            @PathVariable UUID venueCourtId,
            @Valid @RequestBody UpdateVenueCourtRequest request
    ) throws ServiceException {
        UpdateVenueCourtCommand command = new UpdateVenueCourtCommand(
                venueCourtId,
                request.getName(),
                request.getSportType(),
                request.getCapacity(),
                request.getSurfaceType(),
                request.getActive(),
                currentUserId()
        );
        return ResponseEntity.ok(toResponse(venueCourtService.updateVenueCourt(command)));
    }

    @PatchMapping("/venue-courts/{venueCourtId}")
    @PreAuthorize("hasAuthority('CHANGE_OWN_VENUE_COURT_STATUS')")
    public ResponseEntity<VenueCourtResponse> updateVenueCourtActive(
            @PathVariable UUID venueCourtId,
            @Valid @RequestBody UpdateVenueCourtActiveRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(toResponse(venueCourtService.updateActive(
                venueCourtId, request.getActive(), currentUserId()
        )));
    }

    @DeleteMapping("/venue-courts/{venueCourtId}")
    @PreAuthorize("hasAuthority('DELETE_OWN_VENUE_COURT')")
    public ResponseEntity<Void> deleteVenueCourt(@PathVariable UUID venueCourtId) throws ServiceException {
        venueCourtService.softDelete(venueCourtId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    private VenueCourtResponse toResponse(VenueCourt court) {
        return venueCourtConverter.toResponse(venueCourtMapper.toVenueCourtResponseCommand(court));
    }

    private UUID currentUserId() throws ServiceException {
        String value = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        return BaseUtil.uuid(value);
    }
}
