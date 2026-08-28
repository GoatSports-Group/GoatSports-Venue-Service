package iuh.fit.venueservice.infrastructure.persistence.entity;

import iuh.fit.venueservice.domain.enums.CheckInMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "check_in_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRecordEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID checkInId;

    @Column(columnDefinition = "UUID")
    private UUID paymentId;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID checkedInBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CheckInMethod method;

    private BigDecimal remainingAmountCollected;
    private LocalDateTime checkedInAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private BookingEntity booking;
}
