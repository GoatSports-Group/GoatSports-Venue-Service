package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public record CleanupOwnerApplicationUploadCommand(
        UUID ownerApplicationId,
        UUID userId,
        List<String> objectKeys
) {
    public void validate() throws ServiceException {
        if (ownerApplicationId == null || userId == null) {
            throw new ServiceException("Không xác định được phiên tải hồ sơ.");
        }
        if (objectKeys == null || objectKeys.isEmpty() || objectKeys.size() > 4) {
            throw new ServiceException("Danh sách tài liệu tạm không hợp lệ.");
        }
        if (objectKeys.stream().anyMatch(key -> key == null || key.isBlank())) {
            throw new ServiceException("Object key của tài liệu không được để trống.");
        }
        if (objectKeys.stream().distinct().count() != objectKeys.size()) {
            throw new ServiceException("Danh sách tài liệu có object key bị trùng.");
        }
        if (objectKeys.stream().anyMatch(key -> !belongsToUser(key))) {
            throw new ServiceException("Không được phép xóa tài liệu của người dùng khác.");
        }
    }

    private boolean belongsToUser(String key) {
        return key.startsWith("temp/identities/" + userId + "/")
                || key.startsWith("temp/licenses/" + userId + "/")
                || key.startsWith("temp/venues/" + userId + "/");
    }
}
