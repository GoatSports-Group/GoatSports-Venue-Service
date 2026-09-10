package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record GenerateTimeSlotsCommand(
        UUID venueCourtId,
        LocalDate fromDate,
        LocalDate toDate,
        List<UUID> pricingRuleIds,
        Integer slotDurationMinutes,
        UUID currentUserId
) {
    private static final int MAX_GENERATION_DAYS = 31;
    private static final int MIN_SLOT_MINUTES = 30;
    private static final int MAX_SLOT_MINUTES = 240;

    public void validate() throws ServiceException {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new ServiceException("Khoảng ngày không hợp lệ.");
        }
        if (fromDate.isBefore(LocalDate.now())) {
            throw new ServiceException("Không thể sinh slot trong quá khứ.");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) >= MAX_GENERATION_DAYS) {
            throw new ServiceException("Mỗi lần chỉ được quản lý tối đa 31 ngày.");
        }
        if (slotDurationMinutes == null || slotDurationMinutes < MIN_SLOT_MINUTES
                || slotDurationMinutes > MAX_SLOT_MINUTES || slotDurationMinutes % 15 != 0) {
            throw new ServiceException("Thời lượng slot phải từ 30 đến 240 phút và chia hết cho 15.");
        }
        if (pricingRuleIds == null || pricingRuleIds.isEmpty()
                || pricingRuleIds.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("Hãy chọn ít nhất một quy tắc giá để sinh lịch.");
        }
        if (pricingRuleIds.stream().distinct().count() != pricingRuleIds.size()) {
            throw new ServiceException("Danh sách quy tắc giá được chọn không hợp lệ.");
        }
    }
}
