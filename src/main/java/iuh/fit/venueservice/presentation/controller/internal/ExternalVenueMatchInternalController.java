package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.service.ExternalVenueDemandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** ai-service báo một kèo ghép trận đã hoàn tất ở sân ngoài (gateway chặn /internal/** từ ngoài). */
@RestController
@RequestMapping("/internal/external-venues")
@RequiredArgsConstructor
public class ExternalVenueMatchInternalController {
    private final ExternalVenueDemandService demandService;

    /** PUT vì idempotent: cùng phiên báo lại không đếm thêm kèo. */
    @PutMapping("/{externalVenueId}/matches/{sessionId}")
    public ResponseEntity<Void> recordMatch(@PathVariable UUID externalVenueId, @PathVariable UUID sessionId) {
        demandService.recordMatch(sessionId, externalVenueId);
        return ResponseEntity.noContent().build();
    }
}
