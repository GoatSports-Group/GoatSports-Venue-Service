package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.shared.annotation.AuditLog;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueSuggestionReview;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.ExternalVenueDemandService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.request.venue.RejectExternalVenueSuggestionRequest;
import iuh.fit.venueservice.presentation.response.venue.ExternalVenueDemandResponse;
import iuh.fit.venueservice.presentation.response.venue.ExternalVenueResponse;
import iuh.fit.venueservice.presentation.response.venue.ExternalVenueSuggestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Admin duyệt sân người chơi gợi ý và xem danh sách sân tiềm năng (nên mời làm đối tác). */
@RestController
@RequestMapping("/api/v1/admin/external-venues")
@RequiredArgsConstructor
public class ExternalVenueDemandAdminController {
    private final ExternalVenueDemandService demandService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/suggestions")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    public ResponseEntity<BaseListResponse> pendingSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<ExternalVenueSuggestionReview> result = demandService.pendingSuggestions(page, size);
        return ResponseEntity.ok(new BaseListResponse(
                new BaseListResponse.Meta(result.page(), result.pageSize(), result.totalPages(), result.total()),
                result.items().stream().map(ExternalVenueSuggestionResponse::from).toList()
        ));
    }

    /** Số gợi ý chờ duyệt, cho huy hiệu trên tab. */
    @GetMapping("/suggestions/count")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    public ResponseEntity<Map<String, Long>> pendingSuggestionCount() {
        return ResponseEntity.ok(Map.of("pending", demandService.pendingSuggestionCount()));
    }

    @PostMapping("/suggestions/{suggestionId}/approve")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    @AuditLog(action = "ADMIN_APPROVE_VENUE_SUGGESTION", description = "Admin duyệt sân người chơi gợi ý #{#suggestionId}")
    public ResponseEntity<ExternalVenueResponse> approve(@PathVariable UUID suggestionId) throws ServiceException {
        return ResponseEntity.ok(ExternalVenueResponse.from(demandService.approve(suggestionId, currentUserId())));
    }

    @PostMapping("/suggestions/{suggestionId}/reject")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    @AuditLog(action = "ADMIN_REJECT_VENUE_SUGGESTION", description = "Admin từ chối sân người chơi gợi ý #{#suggestionId}")
    public ResponseEntity<Void> reject(
            @PathVariable UUID suggestionId,
            @Valid @RequestBody RejectExternalVenueSuggestionRequest request
    ) throws ServiceException {
        demandService.reject(suggestionId, currentUserId(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/potential")
    @PreAuthorize("hasAuthority('SUSPEND_VENUE')")
    public ResponseEntity<BaseListResponse> potential(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<ExternalVenueDemand> result = demandService.potential(page, size);
        return ResponseEntity.ok(new BaseListResponse(
                new BaseListResponse.Meta(result.page(), result.pageSize(), result.totalPages(), result.total()),
                result.items().stream().map(ExternalVenueDemandResponse::from).toList()
        ));
    }

    private UUID currentUserId() throws ServiceException {
        return BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Không xác định được tài khoản admin.")));
    }
}
