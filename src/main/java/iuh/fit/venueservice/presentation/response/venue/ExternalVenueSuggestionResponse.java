package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueSuggestionReview;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueSuggestionReview.NearbyVenue;
import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;
import iuh.fit.venueservice.domain.enums.SportType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Lượt gợi ý chờ duyệt cho admin; {@code nearby}: sân đã có trong 150 m, để thấy ngay nếu gợi ý trùng. */
public record ExternalVenueSuggestionResponse(
        UUID suggestionId,
        UUID suggestedBy,
        String name,
        List<SportType> sportTypes,
        double latitude,
        double longitude,
        String address,
        String phone,
        String note,
        LocalDateTime createdAt,
        List<NearbyVenue> nearby
) {
    public static ExternalVenueSuggestionResponse from(ExternalVenueSuggestionReview review) {
        ExternalVenueSuggestion suggestion = review.suggestion();
        return new ExternalVenueSuggestionResponse(
                suggestion.getSuggestionId(),
                suggestion.getSuggestedBy(),
                suggestion.getName(),
                suggestion.getSportTypes().stream().sorted().toList(),
                suggestion.getLatitude(),
                suggestion.getLongitude(),
                suggestion.getAddress(),
                suggestion.getPhone(),
                suggestion.getNote(),
                suggestion.getCreatedAt(),
                review.nearby()
        );
    }
}
