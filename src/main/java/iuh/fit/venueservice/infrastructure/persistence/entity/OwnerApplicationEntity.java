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

import static jakarta.persistence.CascadeType.*;

@Entity
@Table(name = "owner_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ownerApplicationDocuments"})
public class OwnerApplicationEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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

    @OneToOne(mappedBy = "ownerApplication", cascade = {PERSIST, MERGE}, orphanRemoval = true)
    private AddressEntity addresses;

    @OneToMany(mappedBy = "ownerApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private Set<OwnerApplicationDocumentEntity> ownerApplicationDocuments = new HashSet<>();

    @OneToOne(mappedBy = "ownerApplication", cascade = {PERSIST, MERGE}, orphanRemoval = true)
    private VenueOwnerProfileEntity venueOwnerProfileEntity;
}