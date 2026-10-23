package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.SuggestExternalVenueCommand;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemandView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.ExternalVenueDemandService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;
import iuh.fit.venueservice.presentation.request.venue.SuggestExternalVenueRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Người chơi góp nhu cầu cho sân ngoài hệ thống: "muốn đặt sân này online" và gợi ý sân còn thiếu. Xem số lượt
 * muốn đặt thì công khai (gateway cho GET qua); bấm muốn đặt và gợi ý sân thì phải đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/venues/external")
@RequiredArgsConstructor
public class ExternalVenueDemandController {
    private final ExternalVenueDemandService demandService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{externalVenueId}/demand")
    public ResponseEntity<ExternalVenueDemandView> demand(@PathVariable UUID externalVenueId) throws ServiceException {
        UUID viewer = currentUserProvider.getCurrentUserId().map(UUID::fromString).orElse(null);
        return ResponseEntity.ok(demandService.demand(externalVenueId, viewer));
    }

    @PutMapping("/{externalVenueId}/want")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExternalVenueDemandView> want(@PathVariable UUID externalVenueId) throws ServiceException {
        return ResponseEntity.ok(demandService.want(externalVenueId, currentUserId()));
    }

    @DeleteMapping("/{externalVenueId}/want")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExternalVenueDemandView> unwant(@PathVariable UUID externalVenueId) throws ServiceException {
        return ResponseEntity.ok(demandService.unwant(externalVenueId, currentUserId()));
    }

    @PostMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, UUID>> suggest(@Valid @RequestBody SuggestExternalVenueRequest request)
            throws ServiceException {
        ExternalVenueSuggestion suggestion = demandService.suggest(currentUserId(), new SuggestExternalVenueCommand(
                request.name(), request.sportTypes(), request.latitude(), request.longitude(),
                request.address(), request.phone(), request.note()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("suggestionId", suggestion.getSuggestionId()));
    }

    private UUID currentUserId() throws ServiceException {
        return BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn cần đăng nhập để làm việc này.")));
    }
}
