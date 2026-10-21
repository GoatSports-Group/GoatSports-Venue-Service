package iuh.fit.venueservice.application.service;

import java.util.List;
import java.util.UUID;

/** Phần của venue-service trong luồng xóa tài khoản (auth-service điều phối). */
public interface AccountDataService {
    /** Mã các việc còn dang dở ở venue-service chặn việc xóa tài khoản. */
    List<String> deletionBlockers(UUID userId);

    /** Tài khoản đã bị ẩn danh hóa: xóa dữ liệu định danh còn lưu ở đây. Gọi lại nhiều lần vẫn an toàn. */
    void eraseUserData(UUID userId);
}
