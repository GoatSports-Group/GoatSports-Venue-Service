package iuh.fit.venueservice.presentation.request.venue;

import iuh.fit.venueservice.domain.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOwnerBookingStatusRequest {
    @NotNull(message = "Trạng thái booking là bắt buộc")
    private BookingStatus status;
}
