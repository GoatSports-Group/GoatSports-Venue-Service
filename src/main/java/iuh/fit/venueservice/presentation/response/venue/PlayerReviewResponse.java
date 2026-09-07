package iuh.fit.venueservice.presentation.response.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerReviewResponse {
    private UUID reviewId;
    private UUID venueId;
    private UUID bookingId;
    private Integer rating;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
