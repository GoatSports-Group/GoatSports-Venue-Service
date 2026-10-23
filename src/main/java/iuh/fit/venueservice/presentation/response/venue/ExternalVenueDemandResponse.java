package iuh.fit.venueservice.presentation.response.venue;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;

/** Một dòng "sân tiềm năng": sân ngoài kèm số người muốn đặt online và số kèo đã chơi ở đó. */
public record ExternalVenueDemandResponse(
        ExternalVenueResponse venue,
        String region,
        long wantCount,
        long matchCount
) {
    public static ExternalVenueDemandResponse from(ExternalVenueDemand demand) {
        return new ExternalVenueDemandResponse(ExternalVenueResponse.from(demand.venue()), demand.venue().getRegion(),
                demand.wantCount(), demand.matchCount());
    }
}
