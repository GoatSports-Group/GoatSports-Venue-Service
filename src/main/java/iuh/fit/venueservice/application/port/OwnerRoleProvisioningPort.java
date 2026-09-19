package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public interface OwnerRoleProvisioningPort {
    void grantVenueOwnerRole(UUID userId) throws ServiceException;
}
