package iuh.fit.venueservice.presentation.response.application;

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
    private String businessType;
    private String taxCode;
    private String identityNumber;
    private String status;
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
        private String documentType;
        private String fileUrl;
    }
}
