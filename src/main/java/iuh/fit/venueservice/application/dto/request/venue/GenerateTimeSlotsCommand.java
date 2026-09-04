package iuh.fit.venueservice.application.dto.request.venue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GenerateTimeSlotsCommand(
        UUID venueCourtId,
        LocalDate fromDate,
        LocalDate toDate,
        List<UUID> pricingRuleIds,
        Integer slotDurationMinutes,
        UUID currentUserId
) {}
