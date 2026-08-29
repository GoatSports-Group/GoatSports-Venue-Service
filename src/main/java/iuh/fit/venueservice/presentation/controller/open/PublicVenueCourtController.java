package iuh.fit.venueservice.presentation.controller.open;

import iuh.fit.venueservice.application.dto.response.venue.TimeSlotResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.VenueCourtMapper;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.application.service.VenueSearchService;
import iuh.fit.venueservice.presentation.converter.VenueCourtConverter;
import iuh.fit.venueservice.presentation.response.venue.TimeSlotResponse;
import iuh.fit.venueservice.presentation.response.venue.VenueCourtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venue-courts")
@RequiredArgsConstructor
public class PublicVenueCourtController {
    private final VenueSearchService venueSearchService;
    private final BookingService bookingService;
    private final VenueCourtMapper venueCourtMapper;
    private final VenueCourtConverter venueCourtConverter;

    @GetMapping("/{venueCourtId}")
    public ResponseEntity<VenueCourtResponse> getVenueCourt(@PathVariable UUID venueCourtId)
            throws ServiceException {
        return ResponseEntity.ok(venueCourtConverter.toResponse(
                venueCourtMapper.toVenueCourtResponseCommand(
                        venueSearchService.getVenueCourt(venueCourtId)
                )
        ));
    }

    @GetMapping("/{venueCourtId}/slots")
    public ResponseEntity<List<TimeSlotResponse>> getCourtSlots(
            @PathVariable UUID venueCourtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) throws ServiceException {
        venueSearchService.getVenueCourt(venueCourtId);
        List<TimeSlotResponse> slots = bookingService.getAvailableSlots(venueCourtId, date).stream()
                .map(this::toTimeSlotResponse)
                .toList();
        return ResponseEntity.ok(slots);
    }

    private TimeSlotResponse toTimeSlotResponse(TimeSlotResponseCommand slot) {
        return TimeSlotResponse.builder()
                .timeSlotId(slot.timeSlotId())
                .venueCourtId(slot.venueCourtId())
                .date(slot.date())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .pricePerHour(slot.pricePerHour())
                .status(slot.status())
                .build();
    }
}
