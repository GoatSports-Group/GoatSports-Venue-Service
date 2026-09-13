package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;

import java.util.Locale;
import java.util.Set;

public record OwnerApplicationUploadDocumentCommand(
        OwnerApplicationDocumentSlot slot,
        String fileName,
        String contentType,
        long fileSize
) {
    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024;
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

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
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new ServiceException("Mỗi tài liệu phải có dung lượng không quá 2 MB.");
        }
        String normalizedContentType = contentType.trim().toLowerCase(Locale.ROOT);
        String normalizedFileName = fileName.trim().toLowerCase(Locale.ROOT);
        if (slot == OwnerApplicationDocumentSlot.BUSINESS_LICENSE) {
            if (!"application/pdf".equals(normalizedContentType) || !normalizedFileName.endsWith(".pdf")) {
                throw new ServiceException("Giấy phép kinh doanh chỉ nhận file PDF.");
            }
        } else {
            boolean extensionMatches = switch (normalizedContentType) {
                case "image/jpeg" -> normalizedFileName.endsWith(".jpg") || normalizedFileName.endsWith(".jpeg");
                case "image/png" -> normalizedFileName.endsWith(".png");
                case "image/webp" -> normalizedFileName.endsWith(".webp");
                default -> false;
            };
            if (!IMAGE_CONTENT_TYPES.contains(normalizedContentType) || !extensionMatches) {
                throw new ServiceException("CCCD và ảnh sân chỉ nhận ảnh JPG, PNG hoặc WebP.");
            }
        }
    }
}
