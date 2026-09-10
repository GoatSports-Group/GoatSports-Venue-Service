package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record OwnerRevenueQuery(
        UUID ownerId,
        UUID venueId,
        LocalDate fromDate,
        LocalDate toDate
) {
    private static final int MAX_DATE_RANGE_DAYS = 366;

    public void validate() throws ServiceException {
        if (ownerId == null || fromDate == null || toDate == null) {
            throw new ServiceException("Owner, từ ngày và đến ngày là bắt buộc.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new ServiceException("Đến ngày không được trước từ ngày.");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > MAX_DATE_RANGE_DAYS) {
            throw new ServiceException("Khoảng thời gian doanh thu tối đa 366 ngày.");
        }
    }
}
