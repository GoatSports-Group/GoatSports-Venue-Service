package iuh.fit.venueservice.presentation.request.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOwnerIdentityRequest {
    @NotBlank
    private String identityFrontKey;
    @NotBlank
    private String identityBackKey;
    @NotEmpty
    @Size(min = 3, max = 12)
    private List<@NotBlank String> livenessFrames;
}
