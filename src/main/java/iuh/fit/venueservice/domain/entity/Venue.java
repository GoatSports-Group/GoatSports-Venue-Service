package iuh.fit.venueservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Venue extends BaseEntity {
    private UUID venueId;
    private String name;
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

    private List<String> imageUrls;
    private List<String> amenities;
    private UUID ownerId;

    private Address address;
    private CancellationPolicy cancellationPolicy;
    @Builder.Default
    private Set<VenueCourt> courts = new HashSet<>();
    @Builder.Default
    private Set<Review> reviews = new HashSet<>();
}
