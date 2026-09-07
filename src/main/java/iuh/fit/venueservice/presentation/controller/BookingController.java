package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.CancelBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateBookingCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.PlayerBookingView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.BookingService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.presentation.converter.BookingConverter;
import iuh.fit.venueservice.presentation.request.venue.CancelBookingRequest;
import iuh.fit.venueservice.presentation.request.venue.CreateBookingRequest;
import iuh.fit.venueservice.presentation.request.venue.ProcessCancellationRequest;
import iuh.fit.venueservice.presentation.response.venue.BookingCancellationResponse;
import iuh.fit.venueservice.presentation.response.venue.BookingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserProvider currentUserProvider;
    private final BookingConverter bookingConverter;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request
    ) throws ServiceException {
        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện đặt sân."));
        UUID userId = BaseUtil.uuid(userIdStr);

        CreateBookingCommand command = new CreateBookingCommand(
                userId,
                request.getVenueCourtId(),
                request.getPlayDate(),
                request.getStartTime(),
                request.getEndTime(),
                BookingSource.DIRECT,
                null,
                null,
                null
        );

        Booking booking = bookingService.createBooking(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingConverter.toResponse(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID id) throws ServiceException {
        return ResponseEntity.ok(bookingConverter.toPlayerResponse(
                bookingService.getPlayerBooking(id, currentUserId("xem chi tiết booking"))
        ));
    }

    @GetMapping("/my-history")
    public ResponseEntity<BaseListResponse> getMyBookingHistory(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws ServiceException {
        PageResult<PlayerBookingView> result = bookingService.getPlayerBookingHistory(
                currentUserId("xem lịch sử đặt sân"), status, page, size
        );
        return ResponseEntity.ok(new BaseListResponse(
                new BaseListResponse.Meta(
                        result.page(), result.pageSize(), result.totalPages(), result.total()
                ),
                result.items().stream().map(bookingConverter::toPlayerResponse).toList()
        ));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingCancellationResponse> cancelBooking(
            @PathVariable UUID id,
            @Valid @RequestBody CancelBookingRequest request
    ) throws ServiceException {
        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để gửi yêu cầu hủy sân."));
        UUID userId = BaseUtil.uuid(userIdStr);

        CancelBookingCommand command = new CancelBookingCommand(id, userId, request.getReason());
        BookingCancellation cancellation = bookingService.requestCancellation(command);

        return ResponseEntity.ok(bookingConverter.toCancellationResponse(cancellation));
    }

    private UUID currentUserId(String action) throws ServiceException {
        return BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để " + action + ".")));
    }

    @PostMapping("/cancellations/{id}/process")
    public ResponseEntity<BookingCancellationResponse> processCancellation(
            @PathVariable UUID id,
            @Valid @RequestBody ProcessCancellationRequest request
    ) throws ServiceException {
        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập với vai trò quản trị/chủ sân."));
        UUID userId = BaseUtil.uuid(userIdStr);

        BookingCancellation cancellation = bookingService.processCancellation(
                id,
                request.getApproved(),
                userId,
                request.getProcessNote()
        );

        return ResponseEntity.ok(bookingConverter.toCancellationResponse(cancellation));
    }
}
