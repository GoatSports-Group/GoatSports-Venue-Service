package iuh.fit.venueservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.UUID;

@Entity
@Table(name = "cancellation_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID policyId;
    private Integer fullRefundHoursBefore;
    private Integer partialRefundHoursBefore;
    private Double partialRefundPercentage;
    private Integer noRefundHoursBefore;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false, unique = true)
    private VenueEntity venue;
}
