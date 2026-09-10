package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.YearMonth;
import java.util.UUID;

public record OwnerCustomerMetricsQuery(
        UUID ownerId,
        UUID venueId,
        YearMonth month
) {
    public void validate() throws ServiceException {
        if (ownerId == null || venueId == null || month == null) {
            throw new ServiceException("Owner, cơ sở và tháng thống kê là bắt buộc.");
        }
    }
}
