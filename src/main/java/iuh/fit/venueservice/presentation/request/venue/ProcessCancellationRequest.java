package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessCancellationRequest {
    @NotNull(message = "Kết quả duyệt không được để trống.")
    private Boolean approved;
    private String processNote;
}
