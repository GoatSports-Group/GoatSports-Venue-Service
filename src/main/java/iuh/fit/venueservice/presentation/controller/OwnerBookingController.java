package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.request.venue.UpdateOwnerBookingStatusCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateOwnerBookingPaymentCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingPaymentResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerBookingService;
import iuh.fit.venueservice.application.service.BookingCancellationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.presentation.request.venue.UpdateOwnerBookingStatusRequest;
import iuh.fit.venueservice.presentation.request.venue.CreateOwnerBookingPaymentRequest;
import iuh.fit.venueservice.presentation.request.venue.ProcessCancellationRequest;
import iuh.fit.venueservice.presentation.request.venue.CompleteManualRefundRequest;
import iuh.fit.venueservice.presentation.response.venue.OwnerBookingResponse;
import iuh.fit.venueservice.presentation.response.venue.BookingCancellationResponse;
import iuh.fit.venueservice.presentation.converter.OwnerBookingResponseMapper;
import iuh.fit.venueservice.presentation.converter.BookingConverter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/bookings")
@RequiredArgsConstructor
public class OwnerBookingController {
    private final OwnerBookingService ownerBookingService;
    private final BookingCancellationService bookingCancellationService;
    private final CurrentUserProvider currentUserProvider;
    private final OwnerBookingResponseMapper responseMapper;
    private final BookingConverter bookingConverter;

    @GetMapping
    @PreAuthorize("hasAuthority('GET_OWN_BOOKINGS')")
    public ResponseEntity<BaseListResponse> getBookings(
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID venueCourtId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws ServiceException {
        PageResult<OwnerBookingView> result = ownerBookingService.getBookings(new OwnerBookingFilter(
                currentUserId(), venueId, venueCourtId, status, query,
                fromDate, toDate, page, size
        ));
        BaseListResponse.Meta meta = new BaseListResponse.Meta(
                result.page(), result.pageSize(), result.totalPages(), result.total()
        );
        return ResponseEntity.ok(new BaseListResponse(
                meta, responseMapper.toResponses(result.items())
        ));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAuthority('GET_OWN_BOOKING')")
    public ResponseEntity<OwnerBookingResponse> getBooking(
            @PathVariable UUID bookingId
    ) throws ServiceException {
        return ResponseEntity.ok(responseMapper.toResponse(ownerBookingService.getBooking(bookingId, currentUserId())));
    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasAuthority('UPDATE_OWN_BOOKING_STATUS')")
    public ResponseEntity<OwnerBookingResponse> updateStatus(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateOwnerBookingStatusRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(responseMapper.toResponse(ownerBookingService.updateStatus(
                new UpdateOwnerBookingStatusCommand(bookingId, request.getStatus(), currentUserId())
        )));
    }

    @PostMapping("/{bookingId}/payments")
    @PreAuthorize("hasAuthority('CREATE_OWN_WALK_IN_BOOKING')")
    public ResponseEntity<OwnerBookingPaymentResult> createPayment(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CreateOwnerBookingPaymentRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(ownerBookingService.createPayment(new CreateOwnerBookingPaymentCommand(
                bookingId, currentUserId(), request.getMethod()
        )));
    }

    @PostMapping("/cancellations/{cancellationId}/process")
    @PreAuthorize("hasAuthority('UPDATE_OWN_BOOKING_STATUS')")
    public ResponseEntity<BookingCancellationResponse> processCancellation(
            @PathVariable UUID cancellationId,
            @Valid @RequestBody ProcessCancellationRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(bookingConverter.toCancellationResponse(
                bookingCancellationService.processCancellation(
                        cancellationId, request.getApproved(), currentUserId(), request.getProcessNote()
                )
        ));
    }

    @PostMapping("/cancellations/{cancellationId}/manual-refund-completion")
    @PreAuthorize("hasAuthority('UPDATE_OWN_BOOKING_STATUS')")
    public ResponseEntity<BookingCancellationResponse> completeManualRefund(
            @PathVariable UUID cancellationId,
            @Valid @RequestBody CompleteManualRefundRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(bookingConverter.toCancellationResponse(
                bookingCancellationService.completeManualRefund(
                        cancellationId, currentUserId(), request.getProviderRefundId()
                )
        ));
    }

    private UUID currentUserId() throws ServiceException {
        return BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để quản lý booking.")));
    }
}
