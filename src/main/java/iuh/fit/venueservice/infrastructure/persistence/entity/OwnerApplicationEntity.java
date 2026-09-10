package iuh.fit.venueservice.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "owner_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "ownerApplicationDocuments", "venue", "address" })
public class OwnerApplicationEntity extends BaseEntity {
    @Id
    @Column(columnDefinition = "UUID")
    private UUID ownerApplicationId;
    private UUID userId;
    private String fullName;
    private String phone;
    private String email;
    private String businessName;
    @Enumerated(EnumType.STRING)
    private BusinessType businessType;
    private String taxCode;
    private String identityNumber;
    @Enumerated(EnumType.STRING)
    private OwnerApplicationStatus status;
    private String rejectReason;
    private UUID reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime viewedAt;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private AddressEntity address;

    @OneToMany(mappedBy = "ownerApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private Set<OwnerApplicationDocumentEntity> ownerApplicationDocuments = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", unique = true)
    private VenueEntity venue;
}
