package iuh.fit.venueservice.infrastructure.persistence.entity;

import iuh.fit.venueservice.domain.enums.CancellationStatus;
import iuh.fit.venueservice.domain.enums.CancellationReviewMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_cancellations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancellationEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID cancellationId;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID requestedByUserId;

    @Column(columnDefinition = "UUID")
    private UUID refundId;

    @Column(nullable = false, length = 1000)
    private String reason;

    private Double refundPercentage;
    private Double refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CancellationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CancellationReviewMode reviewMode;

    @Column(columnDefinition = "UUID")
    private UUID processedBy;

    @Column(length = 1000)
    private String decisionReason;

    @Column(name = "refund_failure_reason", length = 1000)
    private String refundFailureReason;

    private LocalDateTime processedAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private BookingEntity booking;
}
