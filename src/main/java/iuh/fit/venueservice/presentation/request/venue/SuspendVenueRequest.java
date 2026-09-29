package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendVenueRequest(
        @NotBlank(message = "Lý do đình chỉ không được để trống")
        @Size(min = 10, max = 500, message = "Lý do đình chỉ phải từ 10 đến 500 ký tự")
        String reason
) {}
