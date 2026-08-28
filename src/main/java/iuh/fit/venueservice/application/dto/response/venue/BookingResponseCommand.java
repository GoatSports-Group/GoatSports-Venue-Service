package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record BookingResponseCommand(
        UUID bookingId,
        UUID playerId,
        UUID venueCourtId,
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
        LocalDateTime createdAt
) {}
