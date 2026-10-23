package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;

import java.time.LocalDateTime;

/** Một dòng "sân tiềm năng": sân ngoài, số người muốn đặt online, số kèo đã chơi ở đó, và admin đã mời chưa. */
public record ExternalVenueDemandResponse(
        ExternalVenueResponse venue,
        String region,
        long wantCount,
        long matchCount,
        int inviteCount,
        LocalDateTime lastInvitedAt
) {
    public static ExternalVenueDemandResponse from(ExternalVenueDemand demand) {
        return new ExternalVenueDemandResponse(ExternalVenueResponse.from(demand.venue()), demand.venue().getRegion(),
                demand.wantCount(), demand.matchCount(), demand.venue().getInviteCount(),
                demand.venue().getLastInvitedAt());
    }
}
