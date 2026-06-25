package iuh.fit.venueservice.application.port.event;

import iuh.fit.venueservice.application.dto.event.OwnerApplicationEvent;
import iuh.fit.venueservice.domain.entity.OwnerApplication;

public interface OwnerApplicationEventPublisher {
    void publishOwnerApplicationSubmittedAfterCommit(OwnerApplication ownerApplication);

    void publishOwnerApplicationRejectedAfterCommit(OwnerApplication ownerApplication);

    void publishOwnerApplicationApprovedAfterCommit(OwnerApplication ownerApplication);

    void publishOwnerApplicationEvent(String userId, OwnerApplicationEvent event);
}
