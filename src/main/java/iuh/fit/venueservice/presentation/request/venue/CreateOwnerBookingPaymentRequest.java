package iuh.fit.venueservice.presentation.request.venue;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingPaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOwnerBookingPaymentRequest {
    @NotNull
    private OwnerBookingPaymentMethod method;
}
