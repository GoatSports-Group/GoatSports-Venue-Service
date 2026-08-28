package iuh.fit.venueservice.infrastructure.persistence.entity;

import iuh.fit.venueservice.domain.enums.SportType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "venue_courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueCourtEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID venueCourtId;
    @Enumerated(EnumType.STRING)
    private SportType sportType;
    private String name;
    private Integer capacity;
    private String surfaceType;
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<CourtPricingRuleEntity> pricingRules = new HashSet<>();

    @OneToMany(mappedBy = "venueCourt", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<TimeSlotEntity> timeSlots = new HashSet<>();

    @OneToMany(mappedBy = "venueCourt", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<BookingEntity> bookings = new HashSet<>();
}
