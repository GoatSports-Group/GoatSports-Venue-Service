package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BookingQrPort {
    String issue(UUID bookingId, LocalDateTime expiresAt) throws ServiceException;

    UUID verify(String token) throws ServiceException;
}
