package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record CreateWalkInBookingCommand(
        UUID ownerId,
        UUID venueCourtId,
        UUID timeSlotId,
        String customerName,
        String customerPhone
) {
    public void validate() throws ServiceException {
        if (ownerId == null || venueCourtId == null || timeSlotId == null
                || !hasLength(customerName, 2, 120)) {
            throw new ServiceException("Sân, khung giờ và tên khách tại quầy là bắt buộc.");
        }
        if (!normalizedPhone().matches("\\+?[0-9]{8,15}")) {
            throw new ServiceException("Số điện thoại khách tại quầy không hợp lệ.");
        }
    }

    public String normalizedPhone() {
        return customerPhone == null ? "" : customerPhone.replaceAll("[\\s.-]", "");
    }

    private boolean hasLength(String value, int min, int max) {
        return value != null && value.trim().length() >= min && value.trim().length() <= max;
    }
}
