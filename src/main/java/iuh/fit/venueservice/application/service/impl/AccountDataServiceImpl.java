package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.AccountDataService;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class AccountDataServiceImpl implements AccountDataService {
    private static final Logger log = LoggerFactory.getLogger(AccountDataServiceImpl.class);

    /** Vé còn hiệu lực hoặc còn tiền đang hoàn. */
    private static final EnumSet<BookingStatus> OPEN_BOOKINGS = EnumSet.of(
            BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.REFUND_PENDING);
    /** Yêu cầu hủy chưa xong: chờ duyệt, hoặc tiền hoàn chưa về tới người chơi. */
    private static final EnumSet<CancellationStatus> OPEN_CANCELLATIONS = EnumSet.complementOf(
            EnumSet.of(CancellationStatus.REJECTED, CancellationStatus.REFUNDED));

    private final BookingRepository bookingRepository;
    private final BookingCancellationRepository cancellationRepository;
    private final VenueRepository venueRepository;
    private final OwnerApplicationRepository ownerApplicationRepository;
    private final StoragePort storagePort;

    public AccountDataServiceImpl(BookingRepository bookingRepository,
                                  BookingCancellationRepository cancellationRepository,
                                  VenueRepository venueRepository,
                                  OwnerApplicationRepository ownerApplicationRepository,
                                  StoragePort storagePort) {
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.venueRepository = venueRepository;
        this.ownerApplicationRepository = ownerApplicationRepository;
        this.storagePort = storagePort;
    }

    @Override
    public List<String> deletionBlockers(UUID userId) {
        List<String> blockers = new ArrayList<>();
        if (bookingRepository.existsByPlayerIdAndStatusIn(userId, OPEN_BOOKINGS)) blockers.add("OPEN_BOOKINGS");
        if (cancellationRepository.existsByRequesterAndStatusIn(userId, OPEN_CANCELLATIONS)) {
            blockers.add("OPEN_CANCELLATIONS");
        }
        if (venueRepository.existsByOwnerId(userId)) blockers.add("OWNS_VENUES");
        if (ownerApplicationRepository.existsByUserIdAndStatusIn(userId, List.of(OwnerApplicationStatus.PENDING))) {
            blockers.add("OWNER_APPLICATION_PENDING");
        }
        return blockers;
    }

    @Override
    public void eraseUserData(UUID userId) {
        // Đơn đặt sân và đánh giá giữ lại: chỉ còn userId, tên hiển thị lấy từ auth-service là "Người dùng đã xóa".
        for (String key : ownerApplicationRepository.eraseApplicantData(userId)) {
            try {
                storagePort.delete(key);
            } catch (ServiceException | RuntimeException e) {
                // ponytail: storage lỗi thì file giấy tờ mồ côi trong thư mục riêng tư; dọn tay nếu cần.
                log.warn("Could not delete owner application document {} of user {}: {}", key, userId, e.getMessage());
            }
        }
    }
}
