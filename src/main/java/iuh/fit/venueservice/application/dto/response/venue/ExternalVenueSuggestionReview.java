package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;

import java.util.List;

/** Một lượt gợi ý chờ duyệt kèm các sân đã có gần đó, để admin thấy ngay nếu gợi ý trùng. */
public record ExternalVenueSuggestionReview(ExternalVenueSuggestion suggestion, List<NearbyVenue> nearby) {

    /** {@code partner}: cơ sở đối tác (đặt online được) thay vì sân ngoài. */
    public record NearbyVenue(String name, int distanceMeters, boolean partner) {}
}
