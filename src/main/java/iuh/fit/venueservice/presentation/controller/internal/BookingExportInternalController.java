package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.dto.request.venue.AdminBookingFilter;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.AdminBookingService;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.presentation.converter.OwnerBookingResponseMapper;
import iuh.fit.venueservice.presentation.response.venue.OwnerBookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class BookingExportInternalController {
    private static final int MAX_EXPORT_ROWS = 10_000;

    private final AdminBookingService adminBookingService;
    private final OwnerBookingResponseMapper responseMapper;

    @GetMapping("/export")
    public List<OwnerBookingResponse> export(
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID venueCourtId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) throws ServiceException {
        AdminBookingFilter filter = new AdminBookingFilter(null, venueId, venueCourtId, status, query, fromDate, toDate, 0, 1);
        return adminBookingService.exportBookings(filter, MAX_EXPORT_ROWS).stream().map(responseMapper::toResponse).toList();
    }
}
