package iuh.fit.venueservice.presentation.controller.open;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.VenueSearchResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.VenueCourtMapper;
import iuh.fit.venueservice.application.service.VenueSearchService;
import iuh.fit.venueservice.presentation.converter.VenueCourtConverter;
import iuh.fit.venueservice.presentation.converter.VenueSearchConverter;
import iuh.fit.venueservice.presentation.response.venue.VenueCourtResponse;
import iuh.fit.venueservice.presentation.response.venue.VenueDetailResponse;
import iuh.fit.venueservice.presentation.response.venue.VenueSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.venueservice.domain.enums.SportType;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class PublicVenueController {
    private final VenueSearchService venueSearchService;
    private final VenueSearchConverter venueSearchConverter;
    private final VenueCourtMapper venueCourtMapper;
    private final VenueCourtConverter venueCourtConverter;

    @GetMapping
    public ResponseEntity<PageResult<VenueSearchResponse>> getVenues(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SportType sportType,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        SearchVenueCommand command = new SearchVenueCommand(
                keyword, sportType, district, city, latitude, longitude, radiusKm,
                minPrice, maxPrice, minRating, page, size
        );
        PageResult<VenueSearchResponseCommand> result = venueSearchService.searchVenues(command);
        List<VenueSearchResponse> items = result.items().stream()
                .map(venueSearchConverter::toResponse)
                .toList();
        return ResponseEntity.ok(new PageResult<>(
                items, result.total(), result.page(), result.pageSize(), result.totalPages()
        ));
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<VenueDetailResponse> getVenue(@PathVariable UUID venueId)
            throws ServiceException {
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(
                venueSearchService.getVenueDetails(venueId)
        ));
    }

    @GetMapping("/{venueId}/courts")
    public ResponseEntity<List<VenueCourtResponse>> getVenueCourts(@PathVariable UUID venueId)
            throws ServiceException {
        List<VenueCourtResponse> courts = venueSearchService.getVenueCourts(venueId).stream()
                .map(venueCourtMapper::toVenueCourtResponseCommand)
                .map(venueCourtConverter::toResponse)
                .toList();
        return ResponseEntity.ok(courts);
    }
}
