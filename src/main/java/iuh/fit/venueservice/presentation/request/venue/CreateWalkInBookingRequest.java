package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateWalkInBookingRequest {
    @NotNull
    private UUID venueCourtId;
    @NotNull
    private UUID timeSlotId;
    @NotBlank
    @Size(min = 2, max = 120)
    private String customerName;
    @NotBlank
    @Size(max = 30)
    private String customerPhone;
}
