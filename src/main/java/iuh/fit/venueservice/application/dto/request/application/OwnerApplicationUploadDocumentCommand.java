package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;

public record OwnerApplicationUploadDocumentCommand(
        OwnerApplicationDocumentSlot slot,
        String fileName,
        String contentType
) {
    public void validate() throws ServiceException {
        if (slot == null) {
            throw new ServiceException("Loại tài liệu không được để trống.");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new ServiceException("Tên tài liệu không được để trống.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new ServiceException("Content-Type của tài liệu không được để trống.");
        }
    }
}
