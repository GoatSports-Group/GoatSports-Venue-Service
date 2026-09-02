package iuh.fit.venueservice.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "address", "cancellationPolicy", "venueCourts", "reviews" })
public class VenueEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID venueId;
    private String name;
    @Column(length = 2000)
    private String description;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean active;
    private Double minPrice;
    private Double maxPrice;
    private Double averageRating;
    private Integer totalReviews;
    private String phone;
    private String email;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> imageUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> amenities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "facility_layout", columnDefinition = "jsonb")
    private VenueFacilityLayout facilityLayout;

    @Column(nullable = false, columnDefinition = "UUID")
    private UUID ownerId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private AddressEntity address;

    @OneToOne(mappedBy = "venue", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CancellationPolicyEntity cancellationPolicy;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private Set<VenueCourtEntity> venueCourts = new HashSet<>();

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private Set<ReviewEntity> reviews = new HashSet<>();
}
