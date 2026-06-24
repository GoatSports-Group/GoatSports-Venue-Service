package iuh.fit.venueservice.presentation.response.venue;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {
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
}
