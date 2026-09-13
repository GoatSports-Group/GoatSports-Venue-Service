package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record AnalyzeOwnerFaceReadinessCommand(UUID userId, String frame) {
    public void validate() throws ServiceException {
        if (userId == null) {
            throw new ServiceException("Bạn phải đăng nhập để xác minh khuôn mặt.");
        }
        if (frame == null || frame.length() < 32 || frame.length() > 1_100_000
                || !frame.startsWith("data:image/") || !frame.contains(";base64,")) {
            throw new ServiceException("Khung hình camera không hợp lệ.");
        }
    }
}
