package iuh.fit.venueservice.infrastructure.external.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-AI-SERVICE",
        url = "${ai.service.url}",
        path = "/api/v1/ai/identity-verifications"
)
public interface AiIdentityVerificationClient {
    @PostMapping("/readiness")
    FaceReadiness analyzeReadiness(@RequestBody FaceReadinessRequest request);

    @PostMapping
    VerificationEnvelope verify(@RequestBody VerificationRequest request);

    @GetMapping("/{verificationId}")
    VerificationEnvelope get(
            @PathVariable UUID verificationId,
            @RequestParam("subjectId") UUID subjectId
    );

    record VerificationRequest(
            UUID subjectId,
            String identityFrontKey,
            String identityBackKey,
            String identityFrontUrl,
            String identityBackUrl,
            List<String> livenessFrames
    ) {
    }

    record VerificationEnvelope(Verification verification) {
    }

    record FaceReadinessRequest(UUID subjectId, String frame) {
    }

    record FaceReadiness(
            boolean ready,
            String reasonCode,
            String message,
            int faceCount,
            double brightness,
            boolean centered,
            boolean frontal,
            boolean glassesFree,
            boolean eyesOpen,
            boolean maskFree,
            List<Double> faceBox,
            double yawRatio,
            double pitchRatio,
            double rollDegrees
    ) {
    }

    record Verification(
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
    }

    record IdentityDocument(
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

    record Scores(
            Double frontQuality,
            Double backQuality,
            Double ocrConfidence,
            Double faceSimilarity,
            Double livenessScore
    ) {
    }
}
