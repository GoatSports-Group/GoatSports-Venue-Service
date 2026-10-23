package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.ExternalVenue;

/** Sân tiềm năng để mời làm đối tác: bao nhiêu người muốn đặt online, bao nhiêu kèo đã chơi ở đó. */
public record ExternalVenueDemand(ExternalVenue venue, long wantCount, long matchCount) {}
