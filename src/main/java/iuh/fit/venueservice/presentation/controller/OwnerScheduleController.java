package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.GenerateTimeSlotsCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpsertCourtPricingRuleCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerScheduleService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.domain.entity.CourtPricingRule;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.presentation.request.venue.GenerateTimeSlotsRequest;
import iuh.fit.venueservice.presentation.request.venue.UpdateTimeSlotStatusRequest;
import iuh.fit.venueservice.presentation.request.venue.UpsertCourtPricingRuleRequest;
import iuh.fit.venueservice.presentation.response.venue.CourtPricingRuleResponse;
import iuh.fit.venueservice.presentation.response.venue.TimeSlotResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class OwnerScheduleController {
    private final OwnerScheduleService scheduleService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/venue-courts/{venueCourtId}/pricing-rules")
    @PreAuthorize("hasAuthority('GET_OWN_COURT_PRICING_RULES')")
    public ResponseEntity<List<CourtPricingRuleResponse>> getPricingRules(
            @PathVariable UUID venueCourtId
    ) throws ServiceException {
        return ResponseEntity.ok(scheduleService.getPricingRules(venueCourtId, currentUserId())
                .stream().map(this::toPricingResponse).toList());
    }

    @PostMapping("/venue-courts/{venueCourtId}/pricing-rules")
    @PreAuthorize("hasAuthority('CREATE_OWN_COURT_PRICING_RULE')")
    public ResponseEntity<CourtPricingRuleResponse> createPricingRule(
            @PathVariable UUID venueCourtId,
            @Valid @RequestBody UpsertCourtPricingRuleRequest request
    ) throws ServiceException {
        CourtPricingRule result = scheduleService.createPricingRule(
                toPricingCommand(null, venueCourtId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toPricingResponse(result));
    }

    @PutMapping("/venue-courts/{venueCourtId}/pricing-rules/{pricingRuleId}")
    @PreAuthorize("hasAuthority('UPDATE_OWN_COURT_PRICING_RULE')")
    public ResponseEntity<CourtPricingRuleResponse> updatePricingRule(
            @PathVariable UUID pricingRuleId,
            @PathVariable UUID venueCourtId,
            @Valid @RequestBody UpsertCourtPricingRuleRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(toPricingResponse(scheduleService.updatePricingRule(
                toPricingCommand(pricingRuleId, venueCourtId, request)
        )));
    }

    @DeleteMapping("/venue-courts/{venueCourtId}/pricing-rules/{pricingRuleId}")
    @PreAuthorize("hasAuthority('DELETE_OWN_COURT_PRICING_RULE')")
    public ResponseEntity<Void> deletePricingRule(
            @PathVariable UUID venueCourtId, @PathVariable UUID pricingRuleId
    )
            throws ServiceException {
        scheduleService.deletePricingRule(venueCourtId, pricingRuleId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/venue-courts/{venueCourtId}/time-slots")
    @PreAuthorize("hasAuthority('GET_OWN_TIME_SLOTS')")
    public ResponseEntity<List<TimeSlotResponse>> getTimeSlots(
            @PathVariable UUID venueCourtId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) throws ServiceException {
        return ResponseEntity.ok(scheduleService.getTimeSlots(
                venueCourtId, fromDate, toDate, currentUserId()
        ).stream().map(this::toTimeSlotResponse).toList());
    }

    @PostMapping("/venue-courts/{venueCourtId}/time-slots")
    @PreAuthorize("hasAuthority('CREATE_OWN_TIME_SLOTS')")
    public ResponseEntity<List<TimeSlotResponse>> generateTimeSlots(
            @PathVariable UUID venueCourtId,
            @Valid @RequestBody GenerateTimeSlotsRequest request
    ) throws ServiceException {
        GenerateTimeSlotsCommand command = new GenerateTimeSlotsCommand(
                venueCourtId, request.getFromDate(), request.getToDate(),
                request.getPricingRuleIds(), request.getSlotDurationMinutes(), currentUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.generateTimeSlots(command)
                .stream().map(this::toTimeSlotResponse).toList());
    }

    @PatchMapping("/time-slots/{timeSlotId}")
    @PreAuthorize("hasAuthority('UPDATE_OWN_TIME_SLOT_STATUS')")
    public ResponseEntity<TimeSlotResponse> updateTimeSlotStatus(
            @PathVariable UUID timeSlotId,
            @Valid @RequestBody UpdateTimeSlotStatusRequest request
    ) throws ServiceException {
        return ResponseEntity.ok(toTimeSlotResponse(scheduleService.updateTimeSlotStatus(
                timeSlotId, request.getStatus(), currentUserId()
        )));
    }

    @DeleteMapping("/time-slots/{timeSlotId}")
    @PreAuthorize("hasAuthority('DELETE_OWN_TIME_SLOT')")
    public ResponseEntity<Void> deleteTimeSlot(@PathVariable UUID timeSlotId) throws ServiceException {
        scheduleService.deleteTimeSlot(timeSlotId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    private UpsertCourtPricingRuleCommand toPricingCommand(
            UUID pricingRuleId, UUID venueCourtId, UpsertCourtPricingRuleRequest request
    ) throws ServiceException {
        return new UpsertCourtPricingRuleCommand(
                pricingRuleId, venueCourtId, request.getDayOfWeek(), request.getStartTime(),
                request.getEndTime(), request.getPricePerHour(), request.getBasePricePerHour(),
                request.getEffectiveFrom(), request.getEffectiveTo(), currentUserId()
        );
    }

    private CourtPricingRuleResponse toPricingResponse(CourtPricingRule rule) {
        return CourtPricingRuleResponse.builder()
                .pricingRuleId(rule.getPricingRuleId()).courtId(rule.getCourtId())
                .dayOfWeek(rule.getDayOfWeek()).startTime(rule.getStartTime()).endTime(rule.getEndTime())
                .pricePerHour(rule.getPricePerHour()).basePricePerHour(rule.getBasePricePerHour())
                .effectiveFrom(rule.getEffectiveFrom()).effectiveTo(rule.getEffectiveTo()).build();
    }

    private TimeSlotResponse toTimeSlotResponse(TimeSlot slot) {
        return TimeSlotResponse.builder()
                .timeSlotId(slot.getTimeSlotId()).venueCourtId(slot.getVenueCourtId())
                .date(slot.getDate()).startTime(slot.getStartTime()).endTime(slot.getEndTime())
                .pricePerHour(slot.getPricePerHour()).status(slot.getStatus().name()).build();
    }

    private UUID currentUserId() throws ServiceException {
        return BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để quản lý lịch và bảng giá.")));
    }
}
