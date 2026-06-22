package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.SportType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueCourt {
    private UUID venueCourtId;
    private SportType sportType;
    private String name;
    private Integer capacity;
    private Double pricePerHour;
    private Boolean indoor;
    private Boolean active;
}
