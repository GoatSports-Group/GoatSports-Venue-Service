package iuh.fit.venueservice.presentation.response.application;

import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class OwnerIdentityVerificationResponse {
    private UUID verificationId;
    private String status;
    private IdentityDocumentResponse document;
    private OwnerIdentityVerificationCommand.Scores scores;
    private String reasonCode;
    private String message;

    @Getter
    @Builder
    public static class IdentityDocumentResponse {
        private String documentType;
        private String identityNumber;
        private String fullName;
        private LocalDate dateOfBirth;
        private LocalDate expiryDate;
        private String gender;
        private String nationality;
        private String placeOfOrigin;
        private String placeOfResidence;
    }
}
