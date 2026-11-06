package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.util.List;
import java.util.UUID;

/** Các mốc giờ của vòng đời đặt sân (process Camunda gọi; job dự phòng gọi khi process chậm hoặc chưa có). */
public interface BookingLifecycleService {
    /**
     * Ghi EXPIRED / NO_SHOW / COMPLETED nếu đơn còn ở bước tương ứng và đã tới mốc giờ.
     *
     * @return trạng thái của đơn sau khi xử lý (không ghi được thì là trạng thái hiện tại)
     */
    BookingStatus applyTimedState(UUID bookingId, BookingStatus target) throws ServiceException;

    /** Đơn còn đang chạy (chưa tới giờ kết thúc) mà chưa có process. */
    List<UUID> findBookingsWithoutLifecycle(int limit);

    /** Mở process cho đơn chưa có; ném lỗi khi workflow-service chưa sẵn sàng để job dừng lượt này. */
    void adoptIntoLifecycle(UUID bookingId) throws ServiceException;
}
