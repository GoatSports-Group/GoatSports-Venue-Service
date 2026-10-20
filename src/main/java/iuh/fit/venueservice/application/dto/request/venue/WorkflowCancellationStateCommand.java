package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.CancellationStatus;

import java.util.UUID;

/**
 * Trạng thái Camunda vừa quyết định cho yêu cầu hủy. {@code version} là job key (tăng dần trong một process):
 * bản có version nhỏ hơn bản đã ghi là lần ghi đến trễ và bị bỏ qua.
 */
public record WorkflowCancellationStateCommand(
        UUID cancellationId,
        UUID bookingId,
        CancellationStatus status,
        long version,
        UUID processedBy,
        String decisionNote,
        String refundFailureReason
) {
}
