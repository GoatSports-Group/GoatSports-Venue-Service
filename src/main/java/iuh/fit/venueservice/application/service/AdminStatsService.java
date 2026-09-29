package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.response.stats.PlatformBookingStats;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDate;

public interface AdminStatsService {
    PlatformBookingStats getBookingStats(LocalDate fromDate, LocalDate toDate) throws ServiceException;
}
