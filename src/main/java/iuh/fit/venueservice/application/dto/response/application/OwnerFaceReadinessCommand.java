package iuh.fit.venueservice.application.dto.response.application;

import java.util.List;

public record OwnerFaceReadinessCommand(
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
