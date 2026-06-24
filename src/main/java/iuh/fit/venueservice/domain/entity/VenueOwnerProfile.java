package iuh.fit.venueservice.domain.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueOwnerProfile {
    private UUID venueOwnerProfileId;
    private UUID userId;
    private String businessName;
    private String phone;
    private String email;
    private String taxCode;
    private Boolean verified;
    private LocalDateTime verifiedAt;
    private OwnerApplication ownerApplication;
}
