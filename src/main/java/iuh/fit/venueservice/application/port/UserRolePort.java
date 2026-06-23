package iuh.fit.venueservice.application.port;

import java.util.UUID;

public interface UserRolePort {
    void assignVenueOwnerRole(UUID userId);
}
