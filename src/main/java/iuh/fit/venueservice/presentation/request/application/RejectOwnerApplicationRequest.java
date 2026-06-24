package iuh.fit.venueservice.presentation.request.application;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectOwnerApplicationRequest {
    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectReason;
}
