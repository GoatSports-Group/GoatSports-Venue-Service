package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.Set;
import java.util.UUID;

/**
 * Bộ lọc danh sách cơ sở cho admin. {@code status} null là tất cả. {@code ids}: chỉ lấy các cơ sở này
 * (trang khác của admin, vd. Giải đấu, tra tên cơ sở theo lô); null = không lọc.
 */
public record AdminVenueFilter(Status status, String query, Set<UUID> ids, int page, int size) {
    /** ACTIVE: đang mở; INACTIVE: chủ sân tự tắt; SUSPENDED: admin đình chỉ. */
    public enum Status { ACTIVE, INACTIVE, SUSPENDED }

    private static final int MAX_PAGE_SIZE = 50;

    public void validate() throws ServiceException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ServiceException("Trang phải từ 0 và kích thước trang từ 1 đến 50.");
        }
        if (query != null && query.trim().length() > 100) {
            throw new ServiceException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        if (ids != null && ids.size() > MAX_PAGE_SIZE) {
            throw new ServiceException("Chỉ tra cứu tối đa 50 cơ sở mỗi lần.");
        }
    }
}
