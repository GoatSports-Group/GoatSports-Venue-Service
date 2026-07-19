package iuh.fit.venueservice.presentation.request.application;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewOwnerApplicationRequest {
    @NotBlank(message = "Người duyệt không được để trống")
    private String reviewerId;
    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectReason;
}
