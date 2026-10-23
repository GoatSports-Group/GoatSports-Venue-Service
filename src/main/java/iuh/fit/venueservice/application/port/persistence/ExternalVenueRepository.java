package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalVenueRepository {
    /** Thêm mới hoặc cập nhật theo (source, sourceId); giữ nguyên id của sân đã có. Trả số bản ghi đã lưu. */
    int upsertAll(List<ExternalVenue> venues);

    /** Bỏ sân đã có chủ nhận quản lý. {@code sportType}, {@code keyword} có thể null; tối đa {@code limit} sân, theo tên. */
    List<ExternalVenue> findInBounds(GeoBounds bounds, SportType sportType, String keyword, int limit);

    Optional<ExternalVenue> findById(UUID externalVenueId);

    List<ExternalVenueRegionSummary> summarizeByRegion();

    /** Sân đã nhập của một khu vực, theo tên; {@code page} tính từ 0. */
    PageResult<ExternalVenue> findByRegion(String region, int page, int size);

    /** Lưu một sân (sân người chơi gợi ý khi được duyệt); trả bản đã lưu kèm id. */
    ExternalVenue save(ExternalVenue venue);

    /** Xóa mọi sân của khu vực cùng lượt muốn đặt và kèo đã ghi ở đó; trả số sân đã xóa. */
    int deleteByRegion(String region);

    /** Ghi lượt "muốn đặt online"; người đã bấm rồi thì bỏ qua. */
    void addWant(UUID externalVenueId, UUID userId);

    void removeWant(UUID externalVenueId, UUID userId);

    long countWants(UUID externalVenueId);

    long countMatches(UUID externalVenueId);

    /**
     * Gắn sân ngoài với cơ sở đối tác vừa được duyệt. true khi đã gắn (hoặc vốn gắn với chính cơ sở này, duyệt lại),
     * false khi sân không còn hoặc đã thuộc cơ sở khác.
     */
    boolean markClaimed(UUID externalVenueId, UUID venueId);

    /**
     * Cơ sở đối tác vừa mở đặt online: nếu nó thay cho một sân ngoài mà chưa báo người muốn đặt, đánh dấu đã báo và trả
     * id sân ngoài đó (chỉ trả một lần); ngược lại trống.
     */
    Optional<UUID> claimWantersNotification(UUID venueId);

    /** Có người chưa nhận được thông báo: bỏ dấu "đã báo" để lần cơ sở cập nhật kế tiếp thử lại. */
    void releaseWantersNotification(UUID externalVenueId);

    /** Người chơi đã bấm "muốn đặt online" sân này. */
    List<UUID> findWanterIds(UUID externalVenueId);

    Optional<ExternalVenue> recordInvite(UUID externalVenueId);

    /** Xóa tài khoản: bỏ mọi lượt muốn đặt của người này. */
    void removeWantsBy(UUID userId);

    boolean hasWant(UUID externalVenueId, UUID userId);

    /** Một kèo đã hoàn tất ở sân; cùng phiên báo lại không đếm trùng. */
    void recordMatch(UUID sessionId, UUID externalVenueId);

    /** Mọi sân có ít nhất một lượt muốn đặt hoặc một kèo, kèm hai con số đó (chưa sắp xếp). */
    List<ExternalVenueDemand> findDemand();
}
