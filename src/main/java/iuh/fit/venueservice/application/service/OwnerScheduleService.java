package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.GenerateTimeSlotsCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpsertCourtPricingRuleCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.CourtPricingRule;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OwnerScheduleService {
    List<CourtPricingRule> getPricingRules(UUID venueCourtId, UUID currentUserId) throws ServiceException;

    CourtPricingRule createPricingRule(UpsertCourtPricingRuleCommand command) throws ServiceException;

    CourtPricingRule updatePricingRule(UpsertCourtPricingRuleCommand command) throws ServiceException;

    void deletePricingRule(UUID venueCourtId, UUID pricingRuleId, UUID currentUserId) throws ServiceException;

    List<TimeSlot> getTimeSlots(
            UUID venueCourtId, LocalDate fromDate, LocalDate toDate, UUID currentUserId
    ) throws ServiceException;

    List<TimeSlot> generateTimeSlots(GenerateTimeSlotsCommand command) throws ServiceException;

    TimeSlot updateTimeSlotStatus(
            UUID timeSlotId, TimeSlotStatus status, UUID currentUserId
    ) throws ServiceException;

    void deleteTimeSlot(UUID timeSlotId, UUID currentUserId) throws ServiceException;
}
