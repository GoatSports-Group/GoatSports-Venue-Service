package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.dto.response.venue.VenueSearchResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.application.service.VenueSearchService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.presentation.converter.VenueSearchConverter;
import iuh.fit.venueservice.presentation.response.venue.TimeSlotResponse;
import iuh.fit.venueservice.presentation.response.venue.VenueDetailResponse;
import iuh.fit.venueservice.presentation.response.venue.VenueSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueSearchController {

    private final VenueSearchService venueSearchService;
    private final BookingService bookingService;
    private final VenueSearchConverter venueSearchConverter;

    @GetMapping("/search")
    public ResponseEntity<PageResult<VenueSearchResponse>> searchVenues(
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
                keyword,
                sportType,
                district,
                city,
                latitude,
                longitude,
                radiusKm,
                minPrice,
                maxPrice,
                minRating,
                page,
                size
        );

        PageResult<VenueSearchResponseCommand> result = venueSearchService.searchVenues(command);

        List<VenueSearchResponse> responseItems = result.items().stream()
                .map(venueSearchConverter::toResponse)
                .collect(Collectors.toList());

        PageResult<VenueSearchResponse> response = new PageResult<>(
                responseItems,
                result.total(),
                result.page(),
                result.pageSize(),
                result.totalPages()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<VenueDetailResponse> getVenueDetails(@PathVariable UUID id) throws ServiceException {
        Venue venue = venueSearchService.getVenueDetails(id);
        return ResponseEntity.ok(venueSearchConverter.toDetailResponse(venue));
    }

    @GetMapping("/courts/{courtId}/slots")
    public ResponseEntity<List<TimeSlotResponse>> getCourtSlots(
            @PathVariable UUID courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) throws ServiceException {
        List<TimeSlotResponseCommand> slots = bookingService.getAvailableSlots(courtId, date);

        List<TimeSlotResponse> response = slots.stream().map(s -> TimeSlotResponse.builder()
                .timeSlotId(s.timeSlotId())
                .venueCourtId(s.venueCourtId())
                .date(s.date())
                .startTime(s.startTime())
                .endTime(s.endTime())
                .pricePerHour(s.pricePerHour())
                .status(s.status())
                .build()
        ).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
