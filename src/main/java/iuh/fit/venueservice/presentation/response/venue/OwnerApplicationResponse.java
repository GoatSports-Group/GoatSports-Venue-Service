package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.DocumentType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerApplicationResponse {
    private UUID ownerApplicationId;
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

    private AddressResponse address;
    private List<DocumentResponse> documents;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private UUID addressId;
        private String address;
        private String ward;
        private String district;
        private String city;
        private String province;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResponse {
        private UUID ownerApplicationDocumentId;
        private DocumentType documentType;
        private String fileUrl;
    }
}
