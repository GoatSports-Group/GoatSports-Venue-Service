package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.dto.request.venue.WorkflowCancellationStateCommand;
import iuh.fit.venueservice.application.dto.response.venue.RefundSnapshot;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.BookingCancellationService;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/booking-cancellations")
@RequiredArgsConstructor
public class BookingCancellationWorkflowInternalController {
    private final BookingCancellationService bookingCancellationService;

    @PutMapping("/{cancellationId}/state")
    public Map<String, String> applyState(@PathVariable UUID cancellationId, @Valid @RequestBody StateRequest request)
            throws ServiceException {
        BookingCancellation cancellation = bookingCancellationService.applyWorkflowState(new WorkflowCancellationStateCommand(
                cancellationId, request.bookingId(), request.status(), request.version(), request.processedBy(),
                request.decisionNote(), request.refundFailureReason()));
        return Map.of("status", cancellation.getStatus().name());
    }

    @PostMapping("/{cancellationId}/refund")
    public RefundResponse createRefund(@PathVariable UUID cancellationId, @Valid @RequestBody RefundRequest request)
            throws ServiceException {
        RefundSnapshot refund = bookingCancellationService.createWorkflowRefund(cancellationId, request.bookingId(),
                request.processedBy());
        return new RefundResponse(refund.refundId(), refund.status(), refund.failureReason());
    }

    @PostMapping("/{cancellationId}/owner-reminder")
    public Map<String, Boolean> remindOwner(@PathVariable UUID cancellationId, @Valid @RequestBody ReminderRequest request)
            throws ServiceException {
        bookingCancellationService.remindOwnerToDecide(cancellationId, request.bookingId());
        return Map.of("reminded", true);
    }

    public record StateRequest(@NotNull UUID bookingId, @NotNull CancellationStatus status, @NotNull Long version,
                               UUID processedBy, String decisionNote, String refundFailureReason) {
    }

    public record RefundRequest(@NotNull UUID bookingId, UUID processedBy) {
    }

    public record RefundResponse(UUID refundId, String status, String failureReason) {
    }

    public record ReminderRequest(@NotNull UUID bookingId) {
    }
}
