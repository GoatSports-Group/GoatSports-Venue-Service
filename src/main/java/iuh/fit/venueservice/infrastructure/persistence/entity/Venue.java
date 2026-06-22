package iuh.fit.venueservice.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"venueCourts"})
public class Venue extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID venueId;
    private String name;
    private String description;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private Boolean active;
    private Double minPrice;
    private Double maxPrice;
    private Double averageRating;
    private Integer totalReviews;
    private UUID userId;
    private String phone;
    private String email;

    @OneToOne
    @JoinColumn(name = "venue_owner_profile_id", nullable = false, unique = true)
    private VenueOwnerProfile venueOwnerProfile;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<VenueCourt> venueCourts = new HashSet<>();
}
