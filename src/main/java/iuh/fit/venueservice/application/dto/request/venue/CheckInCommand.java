package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record CheckInCommand(
        UUID bookingId,
        String qrCode,
        String bookingCode,
        UUID checkedInBy
) {}
