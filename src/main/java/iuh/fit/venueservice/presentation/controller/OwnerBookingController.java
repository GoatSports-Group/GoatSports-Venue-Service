package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.request.venue.UpdateOwnerBookingStatusCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerBookingService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.presentation.request.venue.UpdateOwnerBookingStatusRequest;
import iuh.fit.venueservice.presentation.response.venue.OwnerBookingResponse;
import iuh.fit.venueservice.presentation.response.venue.OwnerPaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final CurrentUserProvider currentUserProvider;

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
                meta, result.items().stream().map(this::toResponse).toList()
        ));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAuthority('GET_OWN_BOOKING')")
    public ResponseEntity<OwnerBookingResponse> getBooking(
            @PathVariable UUID bookingId
    ) throws ServiceException {
        return ResponseEntity.ok(toResponse(ownerBookingService.getBooking(bookingId, currentUserId())));
    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasAuthority('UPDATE_OWN_BOOKING_STATUS')")
    public ResponseEntity<OwnerBookingResponse> updateStatus(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateOwnerBookingStatusRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(toResponse(ownerBookingService.updateStatus(
                new UpdateOwnerBookingStatusCommand(bookingId, request.getStatus(), currentUserId())
        )));
    }

    private OwnerBookingResponse toResponse(OwnerBookingView view) {
        Booking booking = view.booking();
        return OwnerBookingResponse.builder()
                .bookingId(booking.getBookingId()).playerId(booking.getPlayerId())
                .venueId(view.venueId()).venueCourtId(booking.getVenueCourtId())
                .venueName(view.venueName()).courtName(view.courtName())
                .playDate(booking.getPlayDate()).startTime(booking.getStartTime()).endTime(booking.getEndTime())
                .status(booking.getStatus().name()).source(booking.getSource().name())
                .totalPrice(booking.getTotalPrice()).depositAmount(booking.getDepositAmount())
                .remainingAmount(booking.getRemainingAmount())
                .depositPaymentId(booking.getDepositPaymentId()).remainingPaymentId(booking.getRemainingPaymentId())
                .bookingCode(booking.getBookingCode()).qrCode(booking.getQrCode())
                .walkInCustomerName(booking.getWalkInCustomerName())
                .walkInCustomerPhone(booking.getWalkInCustomerPhone())
                .matchmakingSessionId(booking.getMatchmakingSessionId())
                .createdAt(booking.getCreatedAt()).updatedAt(booking.getUpdatedAt())
                .payments(view.payments().stream().map(payment -> OwnerPaymentResponse.builder()
                        .paymentId(payment.paymentId()).purpose(payment.purpose())
                        .amount(payment.amount()).currency(payment.currency()).status(payment.status())
                        .paidAt(payment.paidAt()).expiresAt(payment.expiresAt())
                        .createdAt(payment.createdAt()).build()).toList())
                .allowedTransitions(view.allowedTransitions().stream().map(Enum::name).toList())
                .build();
    }

    private UUID currentUserId() throws ServiceException {
        return BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để quản lý booking.")));
    }
}
