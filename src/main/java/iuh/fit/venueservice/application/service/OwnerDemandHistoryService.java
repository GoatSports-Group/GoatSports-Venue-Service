package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Lịch sử lấp đầy theo giờ của một cơ sở, đầu vào cho dự báo nhu cầu (ai-service, Chronos-Bolt). */
public interface OwnerDemandHistoryService {
    int MAX_HOURS = 2048;

    /**
     * Tỉ lệ lấp đầy {@code hours} giờ gần nhất (giờ cuối kết thúc ở đầu giờ hiện tại, giờ Việt Nam): số sân-giờ đã được
     * giữ / số sân đang hoạt động, trong [0, 1]. {@code null} cho giờ trước khi cơ sở được tạo (chưa có dữ liệu).
     */
    DemandHistory hourlyOccupancy(UUID ownerId, UUID venueId, int hours) throws ServiceException;

    record DemandHistory(UUID venueId, String venueName, LocalDateTime start, int courtCount,
                         LocalTime openTime, LocalTime closeTime, List<Double> occupancy) {
    }
}
