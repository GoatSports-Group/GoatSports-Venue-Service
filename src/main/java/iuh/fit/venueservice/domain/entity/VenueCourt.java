package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VenueCourt extends BaseEntity {
    private UUID venueCourtId;
    private UUID venueId;
    private SportType sportType;
    private String name;
    private Integer capacity;
    private String surfaceType;
    private Boolean active;
    @Builder.Default
    private Set<CourtPricingRule> pricingRules = new HashSet<>();
    @Builder.Default
    private Set<TimeSlot> timeSlots = new HashSet<>();
    @Builder.Default
    private Set<Booking> bookings = new HashSet<>();
}
