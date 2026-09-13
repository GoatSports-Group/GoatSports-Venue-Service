package iuh.fit.venueservice.presentation.request.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeOwnerFaceReadinessRequest {
    @NotBlank
    @Size(min = 32, max = 1_100_000)
    private String frame;
}
