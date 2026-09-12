package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public record VerifyOwnerIdentityCommand(
        UUID userId,
        UUID ownerApplicationId,
        String identityFrontKey,
        String identityBackKey,
        List<String> livenessFrames
) {
    public void validate() throws ServiceException {
        if (userId == null || ownerApplicationId == null) {
            throw new ServiceException("Không xác định được người dùng hoặc hồ sơ đăng ký.");
        }
        validateObjectKey(identityFrontKey, "IDENTITY_FRONT");
        validateObjectKey(identityBackKey, "IDENTITY_BACK");
        if (livenessFrames == null || livenessFrames.size() < 3 || livenessFrames.size() > 12) {
            throw new ServiceException("Cần từ 3 đến 12 khung hình camera để xác minh người thật.");
        }
        if (livenessFrames.stream().anyMatch(frame -> frame == null
                || !frame.startsWith("data:image/")
                || !frame.contains(";base64,")
                || frame.length() > 2_800_000)) {
            throw new ServiceException("Khung hình xác minh không hợp lệ hoặc vượt quá 2 MB.");
        }
    }

    private void validateObjectKey(String objectKey, String slot) throws ServiceException {
        String expectedPrefix = "temp/identities/%s/".formatted(userId);
        if (objectKey == null || !objectKey.startsWith(expectedPrefix)) {
            throw new ServiceException("Tài liệu " + slot + " không thuộc người dùng hiện tại.");
        }
    }
}
