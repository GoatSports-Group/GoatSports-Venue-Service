package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.application.CleanupOwnerApplicationUploadCommand;
import iuh.fit.venueservice.application.dto.request.application.GetOwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.dto.request.application.PrepareOwnerApplicationUploadCommand;
import iuh.fit.venueservice.application.dto.request.application.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.VerifyOwnerIdentityCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerIdentityVerificationCommand;
import iuh.fit.venueservice.application.dto.response.application.PreparedOwnerApplicationUploadCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.OwnerApplication;

public interface OwnerApplicationRegistrationService {
    PreparedOwnerApplicationUploadCommand prepareUpload(PrepareOwnerApplicationUploadCommand command)
            throws ServiceException;

    OwnerIdentityVerificationCommand verifyIdentity(VerifyOwnerIdentityCommand command) throws ServiceException;

    OwnerIdentityVerificationCommand getIdentityVerification(GetOwnerIdentityVerificationCommand command)
            throws ServiceException;

    OwnerApplication submit(SubmitOwnerApplicationCommand command) throws ServiceException;

    void cleanupUpload(CleanupOwnerApplicationUploadCommand command) throws ServiceException;
}
