package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record PrepareOwnerApplicationUploadCommand(
        UUID userId,
        String idempotencyKey,
        List<OwnerApplicationUploadDocumentCommand> documents
) {
    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");
    private static final Set<OwnerApplicationDocumentSlot> REQUIRED_SLOTS = Set.of(
            OwnerApplicationDocumentSlot.IDENTITY_FRONT,
            OwnerApplicationDocumentSlot.IDENTITY_BACK,
            OwnerApplicationDocumentSlot.BUSINESS_LICENSE,
            OwnerApplicationDocumentSlot.VENUE_PHOTO
    );

    public void validate() throws ServiceException {
        if (userId == null) {
            throw new ServiceException("Không xác định được người đăng ký.");
        }
        if (idempotencyKey == null || !IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey.trim()).matches()) {
            throw new ServiceException("Idempotency-Key phải dài 8-128 ký tự và chỉ chứa chữ, số, '.', '_', ':', '-'.");
        }
        if (documents == null || documents.size() != REQUIRED_SLOTS.size()) {
            throw new ServiceException("Hồ sơ phải có đủ mặt trước, mặt sau giấy tờ, giấy phép và ảnh sân.");
        }
        for (OwnerApplicationUploadDocumentCommand document : documents) {
            if (document == null) {
                throw new ServiceException("Thông tin tài liệu không hợp lệ.");
            }
            document.validate();
        }
        Set<OwnerApplicationDocumentSlot> actualSlots = documents.stream()
                .map(OwnerApplicationUploadDocumentCommand::slot)
                .collect(Collectors.toSet());
        if (!actualSlots.equals(REQUIRED_SLOTS) || actualSlots.size() != documents.size()) {
            throw new ServiceException("Loại tài liệu bị thiếu hoặc trùng lặp.");
        }
    }

    public UUID ownerApplicationId() {
        String source = "owner-application:%s:%s".formatted(userId, idempotencyKey.trim());
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }
}
