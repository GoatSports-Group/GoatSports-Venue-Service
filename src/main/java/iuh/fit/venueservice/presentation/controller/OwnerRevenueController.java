package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.venue.OwnerCustomerMetricsQuery;
import iuh.fit.venueservice.application.dto.request.venue.OwnerRevenueQuery;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCustomerMetricsView;
import iuh.fit.venueservice.application.dto.response.venue.OwnerRevenueView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerRevenueService;
import iuh.fit.venueservice.application.util.BaseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/revenue")
@RequiredArgsConstructor
public class OwnerRevenueController {
    private final OwnerRevenueService ownerRevenueService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @PreAuthorize("hasAuthority('GET_OWN_REVENUE')")
    public ResponseEntity<OwnerRevenueView> getRevenue(
            @RequestParam(required = false) UUID venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) throws ServiceException {
        UUID ownerId = BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để xem doanh thu.")));
        return ResponseEntity.ok(ownerRevenueService.getRevenue(
                new OwnerRevenueQuery(ownerId, venueId, fromDate, toDate)
        ));
    }

    @GetMapping("/customer-metrics")
    @PreAuthorize("hasAuthority('GET_OWN_REVENUE')")
    public ResponseEntity<OwnerCustomerMetricsView> getCustomerMetrics(
            @RequestParam UUID venueId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) throws ServiceException {
        UUID ownerId = BaseUtil.uuid(currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để xem thống kê khách hàng.")));
        return ResponseEntity.ok(ownerRevenueService.getCustomerMetrics(
                new OwnerCustomerMetricsQuery(ownerId, venueId, month)
        ));
    }
}
