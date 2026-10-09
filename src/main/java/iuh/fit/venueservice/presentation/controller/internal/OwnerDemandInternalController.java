package iuh.fit.venueservice.presentation.controller.internal;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerDemandHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** ai-service lấy lịch sử lấp đầy để dự báo nhu cầu; venue-service kiểm quyền sở hữu. Chỉ nhận X-Gateway-Secret. */
@RestController
@RequestMapping("/internal/owner-demand")
@RequiredArgsConstructor
public class OwnerDemandInternalController {
    private final OwnerDemandHistoryService ownerDemandHistoryService;

    @GetMapping("/{venueId}")
    public OwnerDemandHistoryService.DemandHistory history(
            @PathVariable UUID venueId,
            @RequestParam UUID ownerId,
            @RequestParam(defaultValue = "2048") int hours
    ) throws ServiceException {
        return ownerDemandHistoryService.hourlyOccupancy(ownerId, venueId, hours);
    }
}
