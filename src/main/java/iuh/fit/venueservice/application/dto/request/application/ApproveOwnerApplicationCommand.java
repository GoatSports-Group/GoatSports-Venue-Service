package iuh.fit.venueservice.application.dto.request.application;

import java.util.UUID;

public record ApproveOwnerApplicationCommand(
        UUID applicationId,
        UUID reviewerId
) {}
