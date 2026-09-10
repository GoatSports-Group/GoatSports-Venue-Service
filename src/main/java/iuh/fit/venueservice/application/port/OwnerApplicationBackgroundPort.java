package iuh.fit.venueservice.application.port;

import java.util.UUID;

public interface OwnerApplicationBackgroundPort {
    void schedule(UUID ownerApplicationId);
}
