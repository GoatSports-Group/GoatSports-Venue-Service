package iuh.fit.venueservice.presentation.request.venue;

import iuh.fit.venueservice.application.dto.request.venue.PaymentCollectionMode;
import iuh.fit.venueservice.domain.enums.CheckInMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ConfirmOwnerCheckInRequest {
    @NotNull
    private UUID bookingId;
    @NotNull
    private CheckInMethod method;
    @NotNull
    private PaymentCollectionMode paymentMode;
    @Size(max = 500)
    private String qrCode;
    @Size(max = 50)
    private String bookingCode;
}
