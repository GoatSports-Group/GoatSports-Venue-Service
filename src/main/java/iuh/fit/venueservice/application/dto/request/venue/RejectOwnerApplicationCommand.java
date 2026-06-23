package iuh.fit.venueservice.application.dto.request.venue;

import java.util.UUID;

public record RejectOwnerApplicationCommand(
        UUID applicationId,
        UUID reviewerId,
        String rejectReason
) {}
