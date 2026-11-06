package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.BookingSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Booking extends BaseEntity {
    private UUID bookingId;
    private UUID playerId;
    private UUID venueCourtId;
    private LocalDate playDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private BookingSource source;
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
    private Long lifecycleInstanceKey;

    private BookingCancellation cancellation;
    private CheckInRecord checkInRecord;
    private Review review;
}
