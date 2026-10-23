package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueImportStatus;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Sân ngoài thực tế (OpenStreetMap): người chơi tìm, chỉ đường và rủ nhau chơi khi chưa có cơ sở đối tác. */
public interface ExternalVenueService {
    /** Nhập sân của một khu vực ở nền (Overpass có thể mất vài phút); mỗi lúc chỉ một đợt. */
    void startImport(String region, GeoBounds bounds) throws ServiceException;

    /** Chạy một đợt nhập ngay trên luồng hiện tại, từng ô nhỏ; trả kết quả cuối (số sân đã lưu, số ô lỗi). */
    ExternalVenueImportStatus importNow(String region, GeoBounds bounds);

    /** Đợt nhập gần nhất (đang chạy hoặc đã xong); trống khi chưa nhập lần nào từ lúc service khởi động. */
    Optional<ExternalVenueImportStatus> importStatus();

    /** Sân ngoài trong khung bản đồ, bỏ sân trùng chỗ với cơ sở đối tác (sân đó đặt online được). */
    List<ExternalVenue> search(GeoBounds bounds, SportType sportType, String keyword, int limit) throws ServiceException;

    ExternalVenue get(UUID externalVenueId) throws ServiceException;

    List<ExternalVenueRegionSummary> summarize();
}
