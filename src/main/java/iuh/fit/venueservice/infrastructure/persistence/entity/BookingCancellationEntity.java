package iuh.fit.venueservice.infrastructure.persistence.entity;

import iuh.fit.venueservice.domain.enums.CancellationStatus;
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

    @Column(columnDefinition = "UUID")
    private UUID refundId;

    @Column(nullable = false, length = 1000)
    private String reason;

    private Double refundPercentage;
    private Double refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CancellationStatus status;

    @Column(columnDefinition = "UUID")
    private UUID processedBy;

    @Column(length = 1000)
    private String decisionReason;

    private LocalDateTime processedAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private BookingEntity booking;
}
