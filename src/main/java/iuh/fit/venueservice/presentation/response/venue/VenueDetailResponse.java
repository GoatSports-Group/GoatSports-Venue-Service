package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.domain.entity.VenueFacilityLayout;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueDetailResponse {
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
    private String address;
    private String ward;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private List<String> imageUrls;
    private List<String> amenities;
    private VenueFacilityLayout facilityLayout;
    private CancellationPolicyResponse cancellationPolicy;
    private Set<VenueCourtResponse> courts;
    private LocalDateTime suspendedAt;
    private String suspensionReason;
}
