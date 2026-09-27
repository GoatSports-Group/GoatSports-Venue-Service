package iuh.fit.venueservice.presentation.response.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID bookingId;
    private UUID playerId;
    private UUID venueCourtId;
    private UUID venueId;
    private String venueName;
    private String courtName;
    private LocalDate playDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String source;
    private Double totalPrice;
    private Double depositAmount;
    private Double remainingAmount;
    private UUID depositPaymentId;
    private UUID remainingPaymentId;
    private String bookingCode;
    private String qrCode;
    private String walkInCustomerName;
    private String walkInCustomerPhone;
    private UUID matchmakingSessionId;
    private LocalDateTime holdExpiresAt;
    private UUID lateRefundPaymentId;
    private Double lateRefundAmount;
    private LocalDateTime lateRefundAt;
    private LocalDateTime createdAt;
    private CancellationPolicyResponse cancellationPolicy;
    private BookingCancellationResponse cancellation;
    private PlayerReviewResponse review;
}
