package iuh.fit.venueservice.presentation.response.application;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OwnerFaceReadinessResponse {
    private boolean ready;
    private String reasonCode;
    private String message;
    private int faceCount;
    private double brightness;
    private boolean centered;
    private boolean frontal;
    private boolean glassesFree;
    private boolean eyesOpen;
    private boolean maskFree;
    private List<Double> faceBox;
    private double yawRatio;
    private double pitchRatio;
    private double rollDegrees;
}
