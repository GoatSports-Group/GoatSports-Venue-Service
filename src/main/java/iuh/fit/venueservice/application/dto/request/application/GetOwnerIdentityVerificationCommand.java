package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.UUID;

public record GetOwnerIdentityVerificationCommand(UUID verificationId, UUID userId) {
    public void validate() throws ServiceException {
        if (verificationId == null || userId == null) {
            throw new ServiceException("Không xác định được phiên xác minh danh tính.");
        }
    }
}
