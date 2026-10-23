package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueClaimReview;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.OwnerApplication;

import java.util.Optional;
import java.util.UUID;

/**
 * Chủ sân nhận quản lý một sân ngoài hệ thống có sẵn (cold start, giai đoạn 3): hồ sơ chủ sân thường mang thêm
 * {@code claimedExternalVenueId}; duyệt xong thì sân ngoài trỏ sang cơ sở đối tác mới và người đã muốn đặt được báo.
 */
public interface ExternalVenueClaimService {
    /** Trước khi lưu hồ sơ: sân phải còn và chưa có chủ. {@code externalVenueId} null thì bỏ qua (hồ sơ thường). */
    void requireClaimable(UUID externalVenueId) throws ServiceException;

    /**
     * Trong transaction duyệt hồ sơ: gắn sân ngoài với cơ sở vừa tạo. Sân đã thuộc cơ sở khác thì ném lỗi để duyệt
     * rollback (mỗi sân chỉ một chủ). Hồ sơ không nhận sân nào thì bỏ qua.
     */
    void linkClaim(OwnerApplication approved) throws ServiceException;

    /**
     * Cơ sở vừa mở đặt online (chủ sân bật "đang hoạt động"): nếu nó thay cho một sân ngoài, báo một lần cho mọi người đã
     * bấm "muốn đặt online". Không báo lúc duyệt vì khi đó cơ sở chưa mở, trang sân còn 404. Best-effort.
     */
    void notifyWantersWhenOpened(UUID venueId, String venueName);

    /** Dữ liệu khối "Nhận quản lý sân có sẵn" cho admin; trống khi hồ sơ không nhận sân nào. */
    Optional<ExternalVenueClaimReview> review(OwnerApplication application);
}
