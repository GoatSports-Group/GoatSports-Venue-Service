package iuh.fit.venueservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = "venue_owner_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueOwnerProfile extends BaseEntity{
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

    @OneToOne
    @JoinColumn(name = "owner_application_id", nullable = false, unique = true)
    private OwnerApplication ownerApplication;

    @OneToOne(mappedBy = "venueOwnerProfile", cascade = {PERSIST, MERGE})
    private Venue venue;
}
