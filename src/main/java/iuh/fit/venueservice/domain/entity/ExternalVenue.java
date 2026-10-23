package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVenue {
    public static final String SOURCE_OSM = "OSM";

    private UUID externalVenueId;
    private String source;
    private String sourceId;
    private String name;
    private Set<SportType> sportTypes;
    private double latitude;
    private double longitude;
    private String address;
    private String phone;
    private String website;
    private String openingHours;
    private String region;
    private LocalDateTime importedAt;
}
