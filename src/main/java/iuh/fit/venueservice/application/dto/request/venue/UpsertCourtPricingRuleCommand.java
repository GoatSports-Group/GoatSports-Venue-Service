package iuh.fit.venueservice.application.dto.request.venue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record UpsertCourtPricingRuleCommand(
        UUID pricingRuleId,
        UUID venueCourtId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Double pricePerHour,
        Double basePricePerHour,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        UUID currentUserId
) {}
