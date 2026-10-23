package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;
import iuh.fit.venueservice.domain.enums.ExternalVenueSuggestionStatus;

import java.util.Optional;
import java.util.UUID;

public interface ExternalVenueSuggestionRepository {
    ExternalVenueSuggestion save(ExternalVenueSuggestion suggestion);

    Optional<ExternalVenueSuggestion> findById(UUID suggestionId);

    /** Cũ nhất trước, để admin duyệt theo thứ tự gửi; {@code page} tính từ 0. */
    PageResult<ExternalVenueSuggestion> findByStatus(ExternalVenueSuggestionStatus status, int page, int size);

    long countPendingBy(UUID suggestedBy);

    long countByStatus(ExternalVenueSuggestionStatus status);

    /** Xóa tài khoản: bỏ mọi lượt gợi ý của người này (sân đã duyệt là bản ghi riêng, vẫn giữ). */
    void deleteBySuggestedBy(UUID suggestedBy);
}
