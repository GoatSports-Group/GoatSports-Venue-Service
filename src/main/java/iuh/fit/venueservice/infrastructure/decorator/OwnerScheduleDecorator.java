package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.venue.GenerateTimeSlotsCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpsertCourtPricingRuleCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerScheduleService;
import iuh.fit.venueservice.application.service.impl.OwnerScheduleServiceImpl;
import iuh.fit.venueservice.domain.entity.CourtPricingRule;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class OwnerScheduleDecorator implements OwnerScheduleService {
    private final OwnerScheduleServiceImpl delegate;

    @Override
    @Transactional(readOnly = true)
    public List<CourtPricingRule> getPricingRules(UUID venueCourtId, UUID currentUserId)
            throws ServiceException {
        return delegate.getPricingRules(venueCourtId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public CourtPricingRule createPricingRule(UpsertCourtPricingRuleCommand command)
            throws ServiceException {
        return delegate.createPricingRule(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public CourtPricingRule updatePricingRule(UpsertCourtPricingRuleCommand command)
            throws ServiceException {
        return delegate.updatePricingRule(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void deletePricingRule(UUID venueCourtId, UUID pricingRuleId, UUID currentUserId)
            throws ServiceException {
        delegate.deletePricingRule(venueCourtId, pricingRuleId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlot> getTimeSlots(
            UUID venueCourtId, LocalDate fromDate, LocalDate toDate, UUID currentUserId
    ) throws ServiceException {
        return delegate.getTimeSlots(venueCourtId, fromDate, toDate, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public List<TimeSlot> generateTimeSlots(GenerateTimeSlotsCommand command) throws ServiceException {
        return delegate.generateTimeSlots(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public TimeSlot updateTimeSlotStatus(
            UUID timeSlotId, TimeSlotStatus status, UUID currentUserId
    ) throws ServiceException {
        return delegate.updateTimeSlotStatus(timeSlotId, status, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public void deleteTimeSlot(UUID timeSlotId, UUID currentUserId) throws ServiceException {
        delegate.deleteTimeSlot(timeSlotId, currentUserId);
    }
}
