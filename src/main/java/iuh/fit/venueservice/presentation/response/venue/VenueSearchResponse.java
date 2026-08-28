package iuh.fit.venueservice.presentation.response.venue;

import lombok.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSearchResponse {
    private UUID venueId;
    private String name;
    private String description;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Double minPrice;
    private Double maxPrice;
    private Double averageRating;
    private Integer totalReviews;
    private String phone;
    private String address;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private List<String> imageUrls;
    private List<String> amenities;
    private List<String> sportTypes;
    private Integer totalCourts;
    private Double distanceKm;
}
