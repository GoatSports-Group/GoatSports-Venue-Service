package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.ExternalVenueSuggestionStatus;
import iuh.fit.venueservice.domain.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Sân thực tế người chơi gợi ý thêm vì bản đồ còn thiếu. Admin duyệt thì sân thành {@link ExternalVenue} nguồn
 * {@link ExternalVenue#SOURCE_PLAYER}; từ chối thì giữ lại để người gợi ý biết lý do.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVenueSuggestion {
    private UUID suggestionId;
    private UUID suggestedBy;
    private String name;
    private Set<SportType> sportTypes;
    private double latitude;
    private double longitude;
    private String address;
    private String phone;
    private String note;
    private ExternalVenueSuggestionStatus status;
    private String rejectReason;
    /** Sân đã tạo khi duyệt. */
    private UUID externalVenueId;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
