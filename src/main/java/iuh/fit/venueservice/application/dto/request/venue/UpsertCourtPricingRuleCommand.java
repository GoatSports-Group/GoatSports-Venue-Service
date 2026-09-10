package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

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
) {
    public void validate() throws ServiceException {
        if (dayOfWeek == null || startTime == null || endTime == null
                || pricePerHour == null || basePricePerHour == null
                || effectiveFrom == null || effectiveTo == null) {
            throw new ServiceException("Vui lòng nhập đầy đủ thông tin quy tắc giá.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new ServiceException("Giờ bắt đầu phải trước giờ kết thúc.");
        }
        if (pricePerHour <= 0 || basePricePerHour <= 0) {
            throw new ServiceException("Giá theo giờ phải lớn hơn 0.");
        }
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ServiceException("Ngày bắt đầu hiệu lực phải trước hoặc bằng ngày kết thúc.");
        }
        int daysUntilRule = Math.floorMod(
                dayOfWeek.getValue() - effectiveFrom.getDayOfWeek().getValue(), 7
        );
        if (effectiveFrom.plusDays(daysUntilRule).isAfter(effectiveTo)) {
            throw new ServiceException("Khoảng hiệu lực không chứa ngày tương ứng với thứ đã chọn.");
        }
    }
}
