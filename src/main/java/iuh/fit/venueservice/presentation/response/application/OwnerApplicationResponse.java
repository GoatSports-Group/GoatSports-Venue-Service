package iuh.fit.venueservice.presentation.response.application;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerApplicationResponse {
    private UUID ownerApplicationId;
    private UUID venueId;
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
    private LocalDateTime createdAt;
    private UUID reviewerId;
    private LocalDateTime reviewedAt;

    private AddressResponse address;
    private Set<DocumentResponse> documents;

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
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResponse {
        private UUID documentId;
        private String documentType;
        private String fileUrl;
    }
}
