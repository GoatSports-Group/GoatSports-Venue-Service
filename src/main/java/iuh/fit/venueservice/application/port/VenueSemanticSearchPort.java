package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.domain.entity.Venue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cầu nối sang semantic search của ai-service.
 *
 * Semantic search là enhancement: nếu AI hỏng thì search filter truyền thống phải
 * vẫn chạy. Vì vậy findVenueIds trả Optional để phân biệt hai tình huống khác hẳn nhau:
 * empty = AI không dùng được, caller bỏ qua semantic; có giá trị nhưng rỗng = AI chạy
 * và thật sự không khớp venue nào.
 */
public interface VenueSemanticSearchPort {
    Optional<List<UUID>> findVenueIds(String query, int maxResults);

    /** @return false khi ai-service khong nhan (tat, hong); loi da duoc ghi log. */
    boolean index(Venue venue, List<String> sportTypes);
}
