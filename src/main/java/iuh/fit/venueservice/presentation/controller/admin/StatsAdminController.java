package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.venueservice.application.dto.response.stats.PlatformBookingStats;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Số liệu đặt sân toàn nền tảng cho dashboard admin. */
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class StatsAdminController {
    private final AdminStatsService adminStatsService;

    @GetMapping("/bookings")
    @PreAuthorize("hasAuthority('GET_PLATFORM_STATS')")
    public ResponseEntity<PlatformBookingStats> bookings(@RequestParam LocalDate fromDate,
                                                         @RequestParam LocalDate toDate) throws ServiceException {
        return ResponseEntity.ok(adminStatsService.getBookingStats(fromDate, toDate));
    }
}
