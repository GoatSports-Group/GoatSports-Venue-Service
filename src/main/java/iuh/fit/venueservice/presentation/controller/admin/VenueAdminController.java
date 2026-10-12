package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.venueservice.application.dto.request.venue.AdminVenueFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.presentation.converter.VenueSearchConverter;
import iuh.fit.venueservice.presentation.request.venue.SuspendVenueRequest;
import iuh.fit.venueservice.presentation.response.venue.VenueDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
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
            @RequestParam(required = false) AdminVenueFilter.Status status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Set<UUID> ids,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws ServiceException {
        PageResult<Venue> result = venueService.getVenues(new AdminVenueFilter(status, query, ids, ownerId, page, size));
        return ResponseEntity.ok(new PageResult<>(
                result.items().stream().map(venueSearchConverter::toDetailResponse).toList(),
                result.total(), result.page(), result.pageSize(), result.totalPages()
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

    @PostMapping("/{venueId}/suspension")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    @AuditLog(action = "ADMIN_SUSPEND_VENUE", description = "Đình chỉ cơ sở #{#venueId}")
    public ResponseEntity<VenueDetailResponse> suspend(
            @PathVariable UUID venueId,
            @Valid @RequestBody SuspendVenueRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(
                venueService.suspendVenue(venueId, request.reason())
        ));
    }

    @DeleteMapping("/{venueId}/suspension")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    @AuditLog(action = "ADMIN_REINSTATE_VENUE", description = "Gỡ đình chỉ cơ sở #{#venueId}")
    public ResponseEntity<VenueDetailResponse> reinstate(@PathVariable UUID venueId) throws ServiceException {
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(venueService.reinstateVenue(venueId)));
    }
}
