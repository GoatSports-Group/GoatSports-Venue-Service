package iuh.fit.venueservice.application.dto.response.application;

import java.time.LocalDate;
import java.util.UUID;

public record OwnerIdentityVerificationCommand(
        UUID verificationId,
        UUID subjectId,
        String identityFrontKey,
        String identityBackKey,
        String status,
        IdentityDocument document,
        Scores scores,
        String reasonCode,
        String message
) {
    public boolean isVerified() {
        return "VERIFIED".equals(status);
    }

    public record IdentityDocument(
            String documentType,
            String identityNumber,
            String fullName,
            LocalDate dateOfBirth,
            LocalDate expiryDate,
            String gender,
            String nationality,
            String placeOfOrigin,
            String placeOfResidence
    ) {
    }

    public record Scores(
            Double frontQuality,
            Double backQuality,
            Double ocrConfidence,
            Double faceSimilarity,
            Double livenessScore
    ) {
    }
}
