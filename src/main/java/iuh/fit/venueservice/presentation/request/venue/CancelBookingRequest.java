package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelBookingRequest {
    @NotBlank(message = "Lý do hủy sân không được để trống.")
    private String reason;
}
