package iuh.fit.venueservice.presentation.controller.open;

import iuh.fit.venueservice.application.dto.request.venue.PublicReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.PublicReviewView;
import iuh.fit.venueservice.application.dto.response.venue.VenueSearchResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.VenueCourtMapper;
import iuh.fit.venueservice.application.service.ReviewService;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.VenueSearchService;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.presentation.converter.VenueCourtConverter;
import iuh.fit.venueservice.presentation.converter.VenueSearchConverter;
import iuh.fit.venueservice.presentation.response.venue.PublicReviewResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class PublicVenueController {
    private static final int MAX_VENUE_IDS = 50;

    private final VenueSearchService venueSearchService;
    private final VenueSearchConverter venueSearchConverter;
    private final VenueCourtMapper venueCourtMapper;
    private final VenueCourtConverter venueCourtConverter;
    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<PageResult<VenueSearchResponse>> getVenues(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SportType sportType,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Double northLatitude,
            @RequestParam(required = false) Double southLatitude,
            @RequestParam(required = false) Double eastLongitude,
            @RequestParam(required = false) Double westLongitude,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            /** Chi lay cac san nay (vd. trang "Sân đã lưu"), toi da MAX_VENUE_IDS id moi lan. */
            @RequestParam(required = false) List<UUID> ids,
            /** price | rating; bỏ trống = mặc định (gần nhất khi tìm theo bán kính). */
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ServiceException {
        if (ids != null && ids.size() > MAX_VENUE_IDS) {
            throw new ServiceException("Chỉ lấy tối đa " + MAX_VENUE_IDS + " sân mỗi lần.");
        }
        SearchVenueCommand command = new SearchVenueCommand(
                keyword, sportType, district, city, latitude, longitude, radiusKm,
                northLatitude, southLatitude, eastLongitude, westLongitude,
                minPrice, maxPrice, minRating, page, size
        );
        if (ids != null) command = command.withVenueIds(ids.stream().distinct().toList());
        if ("price".equalsIgnoreCase(sort)) command = command.withSort(SearchVenueCommand.Sort.PRICE);
        if ("rating".equalsIgnoreCase(sort)) command = command.withSort(SearchVenueCommand.Sort.RATING);
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
        UUID viewerId = currentUserProvider.getCurrentUserId().map(UUID::fromString).orElse(null);
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(
                venueSearchService.viewVenueDetails(venueId, viewerId)
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

    @GetMapping("/{venueId}/reviews")
    public ResponseEntity<PageResult<PublicReviewResponse>> getVenueReviews(
            @PathVariable UUID venueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ServiceException {
        PageResult<PublicReviewView> result = reviewService.getPublishedReviews(new PublicReviewFilter(venueId, page, size));
        return ResponseEntity.ok(new PageResult<>(
                result.items().stream().map(venueSearchConverter::toResponse).toList(),
                result.total(), result.page(), result.pageSize(), result.totalPages()
        ));
    }
}
