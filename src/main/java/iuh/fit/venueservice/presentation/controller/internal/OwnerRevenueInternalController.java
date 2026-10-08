package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.dto.request.venue.OwnerRevenueQuery;
import iuh.fit.venueservice.application.dto.response.venue.OwnerRevenueView;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** report-service tạo báo cáo định kỳ (không có người dùng đăng nhập). Chỉ nhận service identity (X-Gateway-Secret). */
@RestController
@RequestMapping("/internal/owner-revenue")
@RequiredArgsConstructor
public class OwnerRevenueInternalController {
    private final OwnerRevenueService ownerRevenueService;

    @GetMapping("/owners")
    public List<UUID> owners() {
        return ownerRevenueService.findOwnerIds();
    }

    @GetMapping
    public OwnerRevenueView revenue(
            @RequestParam UUID ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) throws ServiceException {
        return ownerRevenueService.getRevenue(new OwnerRevenueQuery(ownerId, null, fromDate, toDate));
    }
}
