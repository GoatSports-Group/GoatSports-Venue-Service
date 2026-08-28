package iuh.fit.venueservice.presentation.request.venue;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CheckInRequest {
    private UUID bookingId;
    private String qrCode;
    private String bookingCode;
}
