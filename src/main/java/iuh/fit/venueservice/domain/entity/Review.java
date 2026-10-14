package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {
    private UUID reviewId;
    private UUID venueId;
    private UUID playerId;
    private UUID bookingId;
    private Integer rating;
    private String content;
    private ReviewStatus status;
    private AiVerdict aiVerdict;
}
