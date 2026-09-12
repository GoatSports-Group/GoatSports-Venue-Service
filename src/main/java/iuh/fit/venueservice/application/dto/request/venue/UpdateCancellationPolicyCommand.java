package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record UpdateCancellationPolicyCommand(
        UUID venueId,
        UUID currentUserId,
        Integer fullRefundHoursBefore,
        Integer partialRefundHoursBefore,
        Double partialRefundPercentage,
        Integer noRefundHoursBefore
) {
    private static final int MAX_HOURS_BEFORE = 8760;

    public void validate() throws ServiceException {
        if (venueId == null || currentUserId == null) {
            throw new ServiceException("Không xác định được cơ sở hoặc chủ sân.");
        }
        if (fullRefundHoursBefore == null || partialRefundHoursBefore == null
                || partialRefundPercentage == null || noRefundHoursBefore == null) {
            throw new ServiceException("Vui lòng nhập đầy đủ quy tắc hoàn tiền.");
        }
        if (fullRefundHoursBefore < 0 || partialRefundHoursBefore < 0 || noRefundHoursBefore < 0) {
            throw new ServiceException("Số giờ áp dụng hoàn tiền không được âm.");
        }
        if (fullRefundHoursBefore > MAX_HOURS_BEFORE
                || partialRefundHoursBefore > MAX_HOURS_BEFORE
                || noRefundHoursBefore > MAX_HOURS_BEFORE) {
            throw new ServiceException("Số giờ áp dụng hoàn tiền không được vượt quá một năm.");
        }
        if (!Double.isFinite(partialRefundPercentage)
                || partialRefundPercentage < 0 || partialRefundPercentage > 100) {
            throw new ServiceException("Tỷ lệ hoàn một phần phải nằm trong khoảng từ 0 đến 100%.");
        }
        if (fullRefundHoursBefore < partialRefundHoursBefore
                || partialRefundHoursBefore < noRefundHoursBefore) {
            throw new ServiceException(
                    "Mốc hoàn toàn bộ phải lớn hơn hoặc bằng mốc hoàn một phần và mốc không hoàn tiền."
            );
        }
    }
}
