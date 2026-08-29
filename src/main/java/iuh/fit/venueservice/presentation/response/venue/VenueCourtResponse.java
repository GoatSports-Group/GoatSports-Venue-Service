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
    private UUID venueId;
    private String sportType;
    private String name;
    private Integer capacity;
    private String surfaceType;
    private Boolean active;
}
