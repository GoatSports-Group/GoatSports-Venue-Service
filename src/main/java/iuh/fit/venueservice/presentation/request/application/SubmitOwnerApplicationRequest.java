package iuh.fit.venueservice.presentation.request.application;

import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitOwnerApplicationRequest {
    @NotNull
    private UUID ownerApplicationId;
    @NotNull
    private UUID identityVerificationId;
    @NotBlank
    private String fullName;
    @NotBlank
    private String phone;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String identityNumber;
    @NotBlank
    private String businessName;
    @NotNull
    private BusinessType businessType;
    @NotBlank
    private String taxCode;

    private UUID claimedExternalVenueId;
    @NotBlank
    private String address;
    @NotBlank
    private String province;
    private String district;
    @NotBlank
    private String ward;
    @NotBlank
    private String city;
    @DecimalMin("-90")
    @DecimalMax("90")
    private Double latitude;
    @DecimalMin("-180")
    @DecimalMax("180")
    private Double longitude;
    @Valid
    @NotEmpty
    @Size(min = 4, max = 4)
    private List<UploadedDocumentRequest> documents;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadedDocumentRequest {
        @NotNull
        private OwnerApplicationDocumentSlot slot;
        @NotBlank
        private String objectKey;
    }
}
