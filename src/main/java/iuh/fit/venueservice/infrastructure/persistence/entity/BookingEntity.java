package iuh.fit.venueservice.infrastructure.persistence.entity;

import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID bookingId;

    @Column(columnDefinition = "UUID")
    private UUID playerId;

    @Column(nullable = false)
    private LocalDate playDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingSource source;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private Double depositAmount;

    private Double remainingAmount;

    @Column(columnDefinition = "UUID")
    private UUID depositPaymentId;

    @Column(columnDefinition = "UUID")
    private UUID remainingPaymentId;

    @Column(unique = true, length = 20)
    private String bookingCode;

    @Column(length = 500)
    private String qrCode;

    private String walkInCustomerName;
    private String walkInCustomerPhone;

    @Column(columnDefinition = "UUID")
    private UUID matchmakingSessionId;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_court_id", nullable = false)
    private VenueCourtEntity venueCourt;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private BookingCancellationEntity cancellation;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CheckInRecordEntity checkInRecord;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private ReviewEntity review;
}
