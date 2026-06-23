package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.port.UserRolePort;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRoleAdapter implements UserRolePort {
    private final AuthClient authClient;

    @Override
    public void assignVenueOwnerRole(UUID userId) {
        authClient.assignVenueOwnerRole(userId.toString());
    }
}
