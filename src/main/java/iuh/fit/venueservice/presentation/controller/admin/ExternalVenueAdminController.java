package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueImportStatus;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.ExternalVenueService;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.presentation.request.venue.ImportExternalVenuesRequest;
import iuh.fit.venueservice.presentation.response.venue.ExternalVenueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    /** Sân đã nhập của một khu vực, theo tên, để admin xem lại kết quả nhập. */
    @GetMapping
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    public ResponseEntity<BaseListResponse> listByRegion(
            @RequestParam String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<ExternalVenue> result = externalVenueService.listByRegion(region, page, size);
        return ResponseEntity.ok(new BaseListResponse(
                new BaseListResponse.Meta(result.page(), result.pageSize(), result.totalPages(), result.total()),
                result.items().stream().map(ExternalVenueResponse::from).toList()
        ));
    }

    /** Xóa khu vực cùng mọi sân của nó (kể cả lượt muốn đặt online và kèo đã ghi ở các sân đó). */
    @DeleteMapping
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    @AuditLog(action = "ADMIN_DELETE_EXTERNAL_VENUE_REGION", description = "Admin xóa khu vực sân ngoài #{#region}")
    public ResponseEntity<Map<String, Integer>> deleteRegion(@RequestParam String region) throws ServiceException {
        return ResponseEntity.ok(Map.of("deleted", externalVenueService.deleteRegion(region)));
    }

    @GetMapping("/import-status")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    public ResponseEntity<ExternalVenueImportStatus> importStatus() {
        return externalVenueService.importStatus()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
