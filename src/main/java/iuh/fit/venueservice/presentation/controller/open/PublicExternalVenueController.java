package iuh.fit.venueservice.presentation.controller.open;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.ExternalVenueService;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.presentation.response.venue.ExternalVenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Sân ngoài hệ thống trên bản đồ tìm sân; công khai như danh sách cơ sở (khách chưa đăng nhập cũng xem được). */
@RestController
@RequestMapping("/api/v1/venues/external")
@RequiredArgsConstructor
public class PublicExternalVenueController {
    private final ExternalVenueService externalVenueService;

    @GetMapping
    public ResponseEntity<List<ExternalVenueResponse>> search(
            @RequestParam double south,
            @RequestParam double west,
            @RequestParam double north,
            @RequestParam double east,
            @RequestParam(required = false) SportType sportType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "100") int limit
    ) throws ServiceException {
        return ResponseEntity.ok(externalVenueService
                .search(new GeoBounds(south, west, north, east), sportType, keyword, limit)
                .stream().map(ExternalVenueResponse::from).toList());
    }

    @GetMapping("/{externalVenueId}")
    public ResponseEntity<ExternalVenueResponse> get(@PathVariable UUID externalVenueId) throws ServiceException {
        return ResponseEntity.ok(ExternalVenueResponse.from(externalVenueService.get(externalVenueId)));
    }
}
