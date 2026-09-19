package iuh.fit.venueservice.presentation.request.venue;

import iuh.fit.venueservice.domain.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModerateReviewRequest {
    @NotNull(message = "Trạng thái kiểm duyệt không được để trống")
    private ReviewStatus status;
    private String reason;
}
