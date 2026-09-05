package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.ConfirmOwnerCheckInCommand;
import iuh.fit.venueservice.application.dto.request.venue.CreateWalkInBookingCommand;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInLookupCommand;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCheckInView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerCheckInService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.converter.OwnerBookingResponseMapper;
import iuh.fit.venueservice.presentation.request.venue.ConfirmOwnerCheckInRequest;
import iuh.fit.venueservice.presentation.request.venue.CreateWalkInBookingRequest;
import iuh.fit.venueservice.presentation.request.venue.OwnerCheckInLookupRequest;
import iuh.fit.venueservice.presentation.response.venue.OwnerCheckInRecordResponse;
import iuh.fit.venueservice.presentation.response.venue.OwnerCheckInResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;

@RestController
@RequestMapping("/api/v1/owner/check-ins")
@RequiredArgsConstructor
public class OwnerCheckInController {
    private final OwnerCheckInService ownerCheckInService;
    private final CurrentUserProvider currentUserProvider;
    private final OwnerBookingResponseMapper bookingMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('GET_OWN_CHECK_INS')")
    public ResponseEntity<BaseListResponse> history(
            @RequestParam(required = false) UUID venueId,
            @RequestParam(required = false) UUID venueCourtId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) throws ServiceException {
        var result = ownerCheckInService.history(new OwnerCheckInFilter(
                currentUserId(), venueId, venueCourtId, date, page, size
        ));
        return ResponseEntity.ok(new BaseListResponse(
                new BaseListResponse.Meta(
                        result.page(), result.pageSize(), result.totalPages(), result.total()
                ),
                result.items().stream().map(this::toResponse).toList()
        ));
    }

    @PostMapping("/lookup")
    @PreAuthorize("hasAuthority('LOOKUP_OWN_CHECK_IN')")
    public ResponseEntity<OwnerCheckInResponse> lookup(
            @Valid @RequestBody OwnerCheckInLookupRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(toResponse(ownerCheckInService.lookup(new OwnerCheckInLookupCommand(
                currentUserId(), request.getBookingId(), request.getQrCode(), request.getBookingCode()
        ))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIRM_OWN_CHECK_IN')")
    public ResponseEntity<OwnerCheckInResponse> confirm(
            @Valid @RequestBody ConfirmOwnerCheckInRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(toResponse(ownerCheckInService.confirm(new ConfirmOwnerCheckInCommand(
                currentUserId(), request.getBookingId(), request.getMethod(), request.getPaymentMode(),
                request.getQrCode(), request.getBookingCode()
        ))));
    }

    @PostMapping("/walk-ins")
    @PreAuthorize("hasAuthority('CREATE_OWN_WALK_IN_BOOKING')")
    public ResponseEntity<OwnerCheckInResponse> createWalkIn(
            @Valid @RequestBody CreateWalkInBookingRequest request
    ) throws ServiceException {
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(
                ownerCheckInService.createWalkIn(new CreateWalkInBookingCommand(
                        currentUserId(), request.getVenueCourtId(), request.getTimeSlotId(),
                        request.getCustomerName(), request.getCustomerPhone()
                ))
        ));
    }

    private OwnerCheckInResponse toResponse(OwnerCheckInView view) {
        var record = view.bookingRecord();
        var booking = bookingMapper.toResponse(new OwnerBookingView(
                record.booking(), record.venueId(), record.venueName(), record.courtName(),
                view.payments(), List.of()
        ));
        OwnerCheckInRecordResponse checkIn = view.checkInRecord() == null ? null
                : OwnerCheckInRecordResponse.builder()
                .checkInId(view.checkInRecord().getCheckInId())
                .paymentId(view.checkInRecord().getPaymentId())
                .checkedInBy(view.checkInRecord().getCheckedInBy())
                .method(view.checkInRecord().getMethod().name())
                .remainingAmountCollected(view.checkInRecord().getRemainingAmountCollected())
                .checkedInAt(view.checkInRecord().getCheckedInAt())
                .build();
        return OwnerCheckInResponse.builder()
                .booking(booking)
                .checkIn(checkIn)
                .outstandingAmount(view.outstandingAmount())
                .checkInEligible(view.checkInEligible())
                .checkInMessage(view.checkInMessage())
                .build();
    }

    private UUID currentUserId() throws ServiceException {
        return BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để check-in.")));
    }
}
