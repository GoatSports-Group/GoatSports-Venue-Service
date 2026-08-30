package iuh.fit.venueservice.presentation.response.venue;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Builder
public record OwnerBookingResponse(
        UUID bookingId,
        UUID playerId,
        UUID venueId,
        UUID venueCourtId,
        String venueName,
        String courtName,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String source,
        Double totalPrice,
        Double depositAmount,
        Double remainingAmount,
        UUID depositPaymentId,
        UUID remainingPaymentId,
        String bookingCode,
        String qrCode,
        String walkInCustomerName,
        String walkInCustomerPhone,
        UUID matchmakingSessionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OwnerPaymentResponse> payments,
        List<String> allowedTransitions
) {
}
