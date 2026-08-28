package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class OwnerApplication extends BaseEntity {
    private UUID ownerApplicationId;
    private UUID venueId;
    private UUID userId;
    private String fullName;
    private String phone;
    private String email;
    private String businessName;
    private BusinessType businessType;
    private String taxCode;
    private String identityNumber;
    private OwnerApplicationStatus status;
    private String rejectReason;
    private UUID reviewerId;
    private LocalDateTime reviewedAt;
    private Address address;
    private Set<OwnerApplicationDocument> documents;
}
