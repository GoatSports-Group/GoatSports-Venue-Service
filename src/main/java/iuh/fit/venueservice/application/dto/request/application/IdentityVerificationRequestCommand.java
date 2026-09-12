package iuh.fit.venueservice.application.dto.request.application;

import java.util.List;
import java.util.UUID;

public record IdentityVerificationRequestCommand(
        UUID subjectId,
        String identityFrontKey,
        String identityBackKey,
        String identityFrontUrl,
        String identityBackUrl,
        List<String> livenessFrames
) {
}
