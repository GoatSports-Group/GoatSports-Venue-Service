package iuh.fit.venueservice.presentation.request.venue;

import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTimeSlotStatusRequest {
    @NotNull
    private TimeSlotStatus status;
}
