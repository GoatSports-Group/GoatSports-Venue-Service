package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.venueservice.application.dto.request.venue.AdminBookingFilter;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.AdminBookingService;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.presentation.converter.OwnerBookingResponseMapper;
import iuh.fit.venueservice.presentation.response.venue.OwnerBookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** Đơn đặt sân của mọi cơ sở, cho admin nền tảng (chỉ đọc). */
@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
public class BookingAdminController {
    private final AdminBookingService adminBookingService;
    private final OwnerBookingResponseMapper responseMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('GET_ALL_BOOKINGS')")
    public ResponseEntity<BaseListResponse> getBookings(
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID venueCourtId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws ServiceException {
        PageResult<OwnerBookingView> result = adminBookingService.getBookings(
                new AdminBookingFilter(ownerId, venueId, venueCourtId, status, query, fromDate, toDate, page, size));
        BaseListResponse.Meta meta = new BaseListResponse.Meta(
                result.page(), result.pageSize(), result.totalPages(), result.total());
        return ResponseEntity.ok(new BaseListResponse(meta, result.items().stream().map(responseMapper::toResponse).toList()));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAuthority('GET_ANY_BOOKING')")
    public ResponseEntity<OwnerBookingResponse> getBooking(@PathVariable UUID bookingId) throws ServiceException {
        return ResponseEntity.ok(responseMapper.toResponse(adminBookingService.getBooking(bookingId)));
    }
}
