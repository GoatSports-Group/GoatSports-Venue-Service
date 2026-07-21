package iuh.fit.venueservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "venue_owner_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ownerApplication", "venue"})
public class VenueOwnerProfileEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID venueOwnerProfileId;
    private UUID userId;
    private String businessName;
    private String phone;
    private String email;
    private String taxCode;
    private Boolean verified;
    private LocalDateTime verifiedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_application_id", nullable = false, unique = true)
    private OwnerApplicationEntity ownerApplication;

    @OneToOne(mappedBy = "venueOwnerProfile", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private VenueEntity venue;
}
