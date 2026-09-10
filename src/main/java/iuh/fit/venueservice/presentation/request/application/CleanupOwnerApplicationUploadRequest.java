package iuh.fit.venueservice.presentation.request.application;

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
public class CleanupOwnerApplicationUploadRequest {
    @NotNull
    private UUID ownerApplicationId;
    @NotEmpty
    @Size(max = 4)
    private List<@NotBlank String> objectKeys;
}
