package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueImportStatus;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.ExternalVenueService;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.presentation.request.venue.ImportExternalVenuesRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/external-venues")
@RequiredArgsConstructor
public class ExternalVenueAdminController {
    private final ExternalVenueService externalVenueService;

    /** Chạy nền vì Overpass có thể mất vài phút; theo dõi ở {@code GET /import-status}. */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    @AuditLog(action = "ADMIN_IMPORT_EXTERNAL_VENUES", description = "Admin nhập sân OpenStreetMap khu vực #{#request.region()}")
    public ResponseEntity<Void> startImport(@Valid @RequestBody ImportExternalVenuesRequest request)
            throws ServiceException {
        externalVenueService.startImport(request.region(),
                new GeoBounds(request.south(), request.west(), request.north(), request.east()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    public ResponseEntity<List<ExternalVenueRegionSummary>> summary() {
        return ResponseEntity.ok(externalVenueService.summarize());
    }

    @GetMapping("/import-status")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    public ResponseEntity<ExternalVenueImportStatus> importStatus() {
        return externalVenueService.importStatus()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
