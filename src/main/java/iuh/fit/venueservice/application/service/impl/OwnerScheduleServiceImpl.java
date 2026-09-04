package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.GenerateTimeSlotsCommand;
import iuh.fit.venueservice.application.dto.request.venue.UpsertCourtPricingRuleCommand;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.CourtPricingRuleRepository;
import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerScheduleService;
import iuh.fit.venueservice.domain.entity.CourtPricingRule;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class OwnerScheduleServiceImpl implements OwnerScheduleService {
    private static final int MAX_GENERATION_DAYS = 31;
    private static final int MIN_SLOT_MINUTES = 30;
    private static final int MAX_SLOT_MINUTES = 240;

    private final CourtPricingRuleRepository pricingRuleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueRepository venueRepository;

    public OwnerScheduleServiceImpl(
            CourtPricingRuleRepository pricingRuleRepository,
            TimeSlotRepository timeSlotRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public List<CourtPricingRule> getPricingRules(UUID venueCourtId, UUID currentUserId)
            throws ServiceException {
        getOwnedCourt(venueCourtId, currentUserId);
        return pricingRuleRepository.findByCourtId(venueCourtId);
    }

    @Override
    public CourtPricingRule createPricingRule(UpsertCourtPricingRuleCommand command)
            throws ServiceException {
        OwnedCourt owned = getOwnedCourt(command.venueCourtId(), command.currentUserId());
        validatePricingRule(command, owned.venue());
        assertNoPricingOverlap(command, pricingRuleRepository.findByCourtId(command.venueCourtId()));
        return pricingRuleRepository.save(toPricingRule(command));
    }

    @Override
    public CourtPricingRule updatePricingRule(UpsertCourtPricingRuleCommand command)
            throws ServiceException {
        CourtPricingRule existing = pricingRuleRepository.findById(command.pricingRuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc giá."));
        OwnedCourt owned = getOwnedCourt(existing.getCourtId(), command.currentUserId());
        if (!existing.getCourtId().equals(command.venueCourtId())) {
            throw new ServiceException("Không thể chuyển quy tắc giá sang sân thi đấu khác.");
        }
        validatePricingRule(command, owned.venue());
        assertNoPricingOverlap(command, pricingRuleRepository.findByCourtId(existing.getCourtId()));
        existing.setDayOfWeek(command.dayOfWeek());
        existing.setStartTime(command.startTime());
        existing.setEndTime(command.endTime());
        existing.setPricePerHour(command.pricePerHour());
        existing.setBasePricePerHour(command.basePricePerHour());
        existing.setEffectiveFrom(command.effectiveFrom());
        existing.setEffectiveTo(command.effectiveTo());
        return pricingRuleRepository.save(existing);
    }

    @Override
    public void deletePricingRule(UUID venueCourtId, UUID pricingRuleId, UUID currentUserId)
            throws ServiceException {
        CourtPricingRule rule = pricingRuleRepository.findById(pricingRuleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc giá."));
        if (!venueCourtId.equals(rule.getCourtId())) {
            throw new ResourceNotFoundException("Quy tắc giá không thuộc sân thi đấu đã chọn.");
        }
        getOwnedCourt(rule.getCourtId(), currentUserId);
        pricingRuleRepository.deleteById(pricingRuleId);
    }

    @Override
    public List<TimeSlot> getTimeSlots(
            UUID venueCourtId, LocalDate fromDate, LocalDate toDate, UUID currentUserId
    ) throws ServiceException {
        getOwnedCourt(venueCourtId, currentUserId);
        validateDateRange(fromDate, toDate, false);
        return timeSlotRepository.findByVenueCourtIdAndDateBetween(venueCourtId, fromDate, toDate);
    }

    @Override
    public List<TimeSlot> generateTimeSlots(GenerateTimeSlotsCommand command) throws ServiceException {
        OwnedCourt owned = getOwnedCourt(command.venueCourtId(), command.currentUserId());
        validateDateRange(command.fromDate(), command.toDate(), true);
        validateDuration(command.slotDurationMinutes());
        assertOpeningHours(owned.venue());

        List<CourtPricingRule> rules = pricingRuleRepository.findByCourtId(command.venueCourtId());
        if (rules.isEmpty()) {
            throw new ServiceException("Hãy tạo ít nhất một quy tắc giá trước khi sinh lịch.");
        }

        List<TimeSlot> existing = timeSlotRepository.findByVenueCourtIdAndDateBetween(
                command.venueCourtId(), command.fromDate(), command.toDate()
        );
        List<TimeSlot> generated = new ArrayList<>();
        for (LocalDate date = command.fromDate(); !date.isAfter(command.toDate()); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            List<CourtPricingRule> applicable = rules.stream()
                    .filter(rule -> appliesOn(rule, currentDate))
                    .sorted(Comparator.comparing(CourtPricingRule::getStartTime))
                    .toList();
            for (CourtPricingRule rule : applicable) {
                generateRuleSlots(command, currentDate, rule, existing, generated);
            }
        }
        if (generated.isEmpty()) {
            return existing;
        }
        List<TimeSlot> saved = timeSlotRepository.saveAll(generated);
        List<TimeSlot> result = new ArrayList<>(existing);
        result.addAll(saved);
        result.sort(Comparator.comparing(TimeSlot::getDate).thenComparing(TimeSlot::getStartTime));
        return result;
    }

    @Override
    public TimeSlot updateTimeSlotStatus(
            UUID timeSlotId, TimeSlotStatus status, UUID currentUserId
    ) throws ServiceException {
        TimeSlot slot = getOwnedTimeSlot(timeSlotId, currentUserId);
        if (status != TimeSlotStatus.AVAILABLE && status != TimeSlotStatus.MAINTENANCE) {
            throw new ServiceException("Chủ sân chỉ có thể đặt slot ở trạng thái khả dụng hoặc bảo trì.");
        }
        if (slot.getStatus() == TimeSlotStatus.LOCKED || slot.getStatus() == TimeSlotStatus.BOOKED) {
            throw new ServiceException("Không thể thay đổi slot đang được giữ chỗ hoặc đã đặt.");
        }
        slot.setStatus(status);
        return timeSlotRepository.save(slot);
    }

    @Override
    public void deleteTimeSlot(UUID timeSlotId, UUID currentUserId) throws ServiceException {
        TimeSlot slot = getOwnedTimeSlot(timeSlotId, currentUserId);
        if (slot.getStatus() == TimeSlotStatus.LOCKED || slot.getStatus() == TimeSlotStatus.BOOKED) {
            throw new ServiceException("Không thể xóa slot đang được giữ chỗ hoặc đã đặt.");
        }
        timeSlotRepository.deleteById(timeSlotId);
    }

    private void generateRuleSlots(
            GenerateTimeSlotsCommand command,
            LocalDate date,
            CourtPricingRule rule,
            List<TimeSlot> existing,
            List<TimeSlot> generated
    ) throws ServiceException {
        LocalTime cursor = rule.getStartTime();
        while (cursor.isBefore(rule.getEndTime())) {
            LocalTime end = cursor.plusMinutes(command.slotDurationMinutes());
            if (end.isAfter(rule.getEndTime())) end = rule.getEndTime();
            LocalTime slotStart = cursor;
            LocalTime slotEnd = end;
            boolean exactExists = existing.stream().anyMatch(slot -> slot.getDate().equals(date)
                    && slot.getStartTime().equals(slotStart) && slot.getEndTime().equals(slotEnd))
                    || generated.stream().anyMatch(slot -> slot.getDate().equals(date)
                    && slot.getStartTime().equals(slotStart) && slot.getEndTime().equals(slotEnd));
            if (!exactExists) {
                boolean overlapsExisting = existing.stream().anyMatch(slot -> slot.getDate().equals(date)
                        && overlaps(slotStart, slotEnd, slot.getStartTime(), slot.getEndTime()));
                if (overlapsExisting) {
                    cursor = end;
                    continue;
                }
                boolean overlapsGenerated = generated.stream().anyMatch(slot -> slot.getDate().equals(date)
                        && overlaps(slotStart, slotEnd, slot.getStartTime(), slot.getEndTime()));
                if (overlapsGenerated) {
                    throw new ServiceException("Lịch đã có slot chồng lấn; hãy xóa hoặc điều chỉnh lịch hiện tại.");
                }
                TimeSlot slot = TimeSlot.builder()
                        .venueCourtId(command.venueCourtId())
                        .date(date)
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .status(TimeSlotStatus.AVAILABLE)
                        .pricePerHour(rule.getPricePerHour())
                        .build();
                generated.add(slot);
            }
            cursor = end;
        }
    }

    private boolean appliesOn(CourtPricingRule rule, LocalDate date) {
        return rule.getDayOfWeek() == date.getDayOfWeek()
                && !date.isBefore(rule.getEffectiveFrom())
                && !date.isAfter(rule.getEffectiveTo());
    }

    private CourtPricingRule toPricingRule(UpsertCourtPricingRuleCommand command) {
        return CourtPricingRule.builder()
                .courtId(command.venueCourtId())
                .dayOfWeek(command.dayOfWeek())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .pricePerHour(command.pricePerHour())
                .basePricePerHour(command.basePricePerHour())
                .effectiveFrom(command.effectiveFrom())
                .effectiveTo(command.effectiveTo())
                .build();
    }

    private void validatePricingRule(UpsertCourtPricingRuleCommand command, Venue venue)
            throws ServiceException {
        if (command.dayOfWeek() == null || command.startTime() == null || command.endTime() == null
                || command.pricePerHour() == null || command.basePricePerHour() == null
                || command.effectiveFrom() == null || command.effectiveTo() == null) {
            throw new ServiceException("Vui lòng nhập đầy đủ thông tin quy tắc giá.");
        }
        if (!command.startTime().isBefore(command.endTime())) {
            throw new ServiceException("Giờ bắt đầu phải trước giờ kết thúc.");
        }
        if (command.pricePerHour() <= 0 || command.basePricePerHour() <= 0) {
            throw new ServiceException("Giá theo giờ phải lớn hơn 0.");
        }
        if (venue.getMinPrice() == null || venue.getMaxPrice() == null
                || venue.getMinPrice() > venue.getMaxPrice()) {
            throw new ServiceException("Cơ sở chưa thiết lập khoảng giá hợp lệ.");
        }
        if (command.pricePerHour() < venue.getMinPrice()
                || command.pricePerHour() > venue.getMaxPrice()) {
            throw new ServiceException("Giá theo giờ phải nằm trong khoảng giá min-max của cơ sở.");
        }
        if (command.effectiveFrom().isAfter(command.effectiveTo())) {
            throw new ServiceException("Ngày bắt đầu hiệu lực phải trước hoặc bằng ngày kết thúc.");
        }
        assertOpeningHours(venue);
        if (command.startTime().isBefore(venue.getOpenTime()) || command.endTime().isAfter(venue.getCloseTime())) {
            throw new ServiceException("Khung giờ giá phải nằm trong giờ hoạt động của cơ sở.");
        }
    }

    private void assertNoPricingOverlap(
            UpsertCourtPricingRuleCommand candidate, List<CourtPricingRule> existing
    ) throws ServiceException {
        boolean overlap = existing.stream()
                .filter(rule -> candidate.pricingRuleId() == null
                        || !candidate.pricingRuleId().equals(rule.getPricingRuleId()))
                .anyMatch(rule -> rule.getDayOfWeek() == candidate.dayOfWeek()
                        && datesOverlap(candidate.effectiveFrom(), candidate.effectiveTo(),
                        rule.getEffectiveFrom(), rule.getEffectiveTo())
                        && overlaps(candidate.startTime(), candidate.endTime(),
                        rule.getStartTime(), rule.getEndTime()));
        if (overlap) {
            throw new ServiceException("Quy tắc giá bị chồng lấn thời gian hoặc ngày hiệu lực.");
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate, boolean futureOnly)
            throws ServiceException {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ServiceException("Khoảng ngày không hợp lệ.");
        }
        if (futureOnly && fromDate.isBefore(LocalDate.now())) {
            throw new ServiceException("Không thể sinh slot trong quá khứ.");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) >= MAX_GENERATION_DAYS) {
            throw new ServiceException("Mỗi lần chỉ được quản lý tối đa 31 ngày.");
        }
    }

    private void validateDuration(Integer durationMinutes) throws ServiceException {
        if (durationMinutes == null || durationMinutes < MIN_SLOT_MINUTES
                || durationMinutes > MAX_SLOT_MINUTES || durationMinutes % 15 != 0) {
            throw new ServiceException("Thời lượng slot phải từ 30 đến 240 phút và chia hết cho 15.");
        }
    }

    private void assertOpeningHours(Venue venue) throws ServiceException {
        if (venue.getOpenTime() == null || venue.getCloseTime() == null
                || !venue.getOpenTime().isBefore(venue.getCloseTime())) {
            throw new ServiceException("Cơ sở chưa có giờ hoạt động hợp lệ.");
        }
    }

    private TimeSlot getOwnedTimeSlot(UUID timeSlotId, UUID currentUserId) throws ServiceException {
        TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy slot thời gian."));
        getOwnedCourt(slot.getVenueCourtId(), currentUserId);
        return slot;
    }

    private OwnedCourt getOwnedCourt(UUID venueCourtId, UUID currentUserId) throws ServiceException {
        VenueCourt court = venueCourtRepository.findById(venueCourtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân thi đấu."));
        Venue venue = venueRepository.findById(court.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở thể thao."));
        if (venue.getOwnerId() == null || !venue.getOwnerId().equals(currentUserId)) {
            throw new OwnershipException("Bạn không có quyền quản lý lịch của cơ sở này.");
        }
        return new OwnedCourt(court, venue);
    }

    private boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private boolean datesOverlap(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        return !startA.isAfter(endB) && !startB.isAfter(endA);
    }

    private record OwnedCourt(VenueCourt court, Venue venue) {}
}
