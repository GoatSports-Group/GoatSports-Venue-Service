package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.VenueCourtMapper;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.presentation.converter.VenueCourtConverter;
import iuh.fit.venueservice.presentation.response.venue.VenueCourtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class VenueCourtAdminController {
    private final VenueCourtService venueCourtService;
    private final VenueCourtMapper venueCourtMapper;
    private final VenueCourtConverter venueCourtConverter;

    @GetMapping("/venues/{venueId}/courts")
    @PreAuthorize("hasAuthority('GET_ANY_VENUE_COURTS')")
    public ResponseEntity<List<VenueCourtResponse>> getVenueCourts(@PathVariable UUID venueId)
            throws ServiceException {
        List<VenueCourtResponse> response = venueCourtService.getVenueCourts(venueId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/venue-courts/{venueCourtId}")
    @PreAuthorize("hasAuthority('GET_ANY_VENUE_COURT')")
    public ResponseEntity<VenueCourtResponse> getVenueCourt(@PathVariable UUID venueCourtId)
            throws ServiceException {
        return ResponseEntity.ok(toResponse(venueCourtService.getVenueCourt(venueCourtId)));
    }

    private VenueCourtResponse toResponse(VenueCourt court) {
        return venueCourtConverter.toResponse(venueCourtMapper.toVenueCourtResponseCommand(court));
    }
}
