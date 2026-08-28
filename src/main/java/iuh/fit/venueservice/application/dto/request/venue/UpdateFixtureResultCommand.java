package iuh.fit.venueservice.application.dto.request.venue;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFixtureResultCommand {
    private UUID fixtureId;
    private UUID actorUserId;
    private Integer score1;
    private Integer score2;
}
