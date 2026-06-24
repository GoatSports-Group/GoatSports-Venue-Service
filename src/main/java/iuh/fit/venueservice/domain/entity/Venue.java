package iuh.fit.venueservice.domain.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {
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
    private String phone;
    private String email;

    private VenueOwnerProfile venueOwnerProfile;
    private Set<VenueCourt> courts;
}
