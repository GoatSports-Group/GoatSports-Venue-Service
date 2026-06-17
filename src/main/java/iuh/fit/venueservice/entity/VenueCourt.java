package iuh.fit.venueservice.entity;

import iuh.fit.venueservice.enums.SportType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "venue_courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueCourt extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID venueCourtId;
    @Enumerated(EnumType.STRING)
    private SportType sportType;
    private String name;
    private Integer capacity;
    private Double pricePerHour;
    private Boolean indoor;
    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;
}
