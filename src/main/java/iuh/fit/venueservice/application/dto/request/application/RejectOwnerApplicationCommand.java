package iuh.fit.venueservice.application.dto.request.application;

import java.util.UUID;

public record RejectOwnerApplicationCommand(
        UUID applicationId,
        UUID reviewerId,
        String rejectReason
) {}
