package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.request.application.IdentityVerificationRequestCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public interface IdentityVerificationPort {
    OwnerIdentityVerificationCommand verify(IdentityVerificationRequestCommand command) throws ServiceException;

    OwnerIdentityVerificationCommand get(UUID verificationId, UUID subjectId) throws ServiceException;
}
