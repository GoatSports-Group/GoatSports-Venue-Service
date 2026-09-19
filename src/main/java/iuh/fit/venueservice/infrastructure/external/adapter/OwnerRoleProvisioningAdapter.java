package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.OwnerRoleProvisioningPort;
import iuh.fit.venueservice.infrastructure.external.client.AuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerRoleProvisioningAdapter implements OwnerRoleProvisioningPort {
    private final AuthClient authClient;

    @Override
    public void grantVenueOwnerRole(UUID userId) throws ServiceException {
        if (userId == null) {
            throw new ServiceException("Không xác định được người dùng để cấp quyền chủ sân.");
        }
        try {
            authClient.assignVenueOwnerRole(userId.toString());
        } catch (Exception exception) {
            throw new ServiceException(
                    "Không cấp được quyền chủ sân cho người dùng " + userId + ": " + exception.getMessage()
            );
        }
    }
}
