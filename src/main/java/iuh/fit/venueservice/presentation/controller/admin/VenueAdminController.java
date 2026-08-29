package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.presentation.converter.VenueSearchConverter;
import iuh.fit.venueservice.presentation.response.venue.VenueDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/venues")
@RequiredArgsConstructor
public class VenueAdminController {
    private final VenueService venueService;
    private final VenueSearchConverter venueSearchConverter;

    @GetMapping
    @PreAuthorize("hasAuthority('GET_ALL_VENUES')")
    public ResponseEntity<PageResult<VenueDetailResponse>> getVenues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<Venue> result = venueService.getVenues(page, size);
        List<VenueDetailResponse> items = result.items().stream()
                .map(venueSearchConverter::toDetailResponse)
                .toList();
        return ResponseEntity.ok(new PageResult<>(
                items, result.total(), result.page(), result.pageSize(), result.totalPages()
        ));
    }

    @GetMapping("/{venueId}")
    @PreAuthorize("hasAuthority('GET_ANY_VENUE')")
    public ResponseEntity<VenueDetailResponse> getVenue(@PathVariable UUID venueId)
            throws ServiceException {
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(
                venueService.getVenue(venueId)
        ));
    }
}
