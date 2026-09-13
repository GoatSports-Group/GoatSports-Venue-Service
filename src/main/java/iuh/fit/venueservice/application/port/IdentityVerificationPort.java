package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.request.application.IdentityVerificationRequestCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerFaceReadinessCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public interface IdentityVerificationPort {
    OwnerFaceReadinessCommand analyzeReadiness(UUID subjectId, String frame) throws ServiceException;

    OwnerIdentityVerificationCommand verify(IdentityVerificationRequestCommand command) throws ServiceException;

    OwnerIdentityVerificationCommand get(UUID verificationId, UUID subjectId) throws ServiceException;
}
