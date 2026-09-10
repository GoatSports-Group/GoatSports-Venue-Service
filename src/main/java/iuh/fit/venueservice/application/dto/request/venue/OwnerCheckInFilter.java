package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDate;
import java.util.UUID;

public record OwnerCheckInFilter(
        UUID ownerId,
        UUID venueId,
        UUID venueCourtId,
        LocalDate date,
        int page,
        int size
) {
    public void validate() throws ServiceException {
        if (ownerId == null) throw new ServiceException("Không xác định được chủ sân.");
        if (page < 0 || size < 1 || size > 20) {
            throw new ServiceException("Trang phải từ 0 và kích thước trang từ 1 đến 20.");
        }
    }
}
