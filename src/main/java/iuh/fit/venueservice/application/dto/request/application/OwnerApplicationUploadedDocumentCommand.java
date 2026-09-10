package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;

import java.util.UUID;

public record OwnerApplicationUploadedDocumentCommand(
        OwnerApplicationDocumentSlot slot,
        String objectKey
) {
    public void validate(UUID userId) throws ServiceException {
        if (slot == null) {
            throw new ServiceException("Loại tài liệu không được để trống.");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new ServiceException("Object key của tài liệu không được để trống.");
        }
        String expectedPrefix = "temp/%s/%s/".formatted(folder(), userId);
        if (!objectKey.startsWith(expectedPrefix)) {
            throw new ServiceException("Tài liệu không thuộc người dùng hoặc không đúng loại tài liệu.");
        }
    }

    private String folder() {
        return switch (slot) {
            case IDENTITY_FRONT, IDENTITY_BACK -> "identities";
            case BUSINESS_LICENSE -> "licenses";
            case VENUE_PHOTO -> "venues";
        };
    }
}
