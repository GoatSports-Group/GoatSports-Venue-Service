package iuh.fit.venueservice.presentation.response.venue;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueCourtResponse {
    private UUID venueCourtId;
    private String sportType;
    private String name;
    private Integer capacity;
    private Double pricePerHour;
    private Boolean indoor;
    private Boolean active;
}
