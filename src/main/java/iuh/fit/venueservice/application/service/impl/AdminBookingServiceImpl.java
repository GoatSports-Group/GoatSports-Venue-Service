package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.AdminBookingFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingView;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.service.AdminBookingService;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {
    private static final int PAYMENT_BATCH = 100;
    private static final int CANCELLATION_BATCH = 1_000;
    private final BookingRepository bookingRepository;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final PaymentQueryPort paymentQueryPort;

    @Override
    public PageResult<OwnerBookingView> getBookings(AdminBookingFilter filter) throws ServiceException {
        filter.validate();
        PageResult<OwnerBookingRecord> page = bookingRepository.findAllBookings(filter);
        Set<UUID> bookingIds = page.items().stream()
                .map(item -> item.booking().getBookingId()).collect(Collectors.toSet());
        Map<UUID, List<PaymentSnapshot>> payments = bookingIds.isEmpty() ? Collections.emptyMap()
                : paymentQueryPort.findByBookingIds(bookingIds).stream()
                .collect(Collectors.groupingBy(PaymentSnapshot::referenceId));
        var cancellations = bookingCancellationRepository.findByBookingIds(bookingIds);
        List<OwnerBookingView> items = page.items().stream()
                .map(item -> view(item, cancellations.get(item.booking().getBookingId()),
                        payments.getOrDefault(item.booking().getBookingId(), List.of())))
                .toList();
        return new PageResult<>(items, page.total(), page.page(), page.pageSize(), page.totalPages());
    }

    @Override
    public List<OwnerBookingView> exportBookings(AdminBookingFilter filter, int maxRows) throws ServiceException {
        filter.validateCriteria();
        PageResult<OwnerBookingRecord> page = bookingRepository.findAllBookings(filter.forExport(maxRows));
        if (page.total() > maxRows) {
            throw new ServiceException("Báo cáo vượt quá " + String.format("%,d", maxRows).replace(',', '.')
                    + " đơn. Hãy thu hẹp khoảng ngày hoặc chọn một cơ sở.");
        }
        List<UUID> bookingIds = page.items().stream().map(item -> item.booking().getBookingId()).toList();
        Map<UUID, List<PaymentSnapshot>> payments = new HashMap<>();
        for (List<UUID> batch : batches(bookingIds, PAYMENT_BATCH)) {
            paymentQueryPort.findByBookingIds(new HashSet<>(batch))
                    .forEach(payment -> payments.computeIfAbsent(payment.referenceId(), id -> new ArrayList<>()).add(payment));
        }
        Map<UUID, BookingCancellation> cancellations = new HashMap<>();
        for (List<UUID> batch : batches(bookingIds, CANCELLATION_BATCH)) {
            cancellations.putAll(bookingCancellationRepository.findByBookingIds(new HashSet<>(batch)));
        }
        return page.items().stream()
                .map(item -> view(item, cancellations.get(item.booking().getBookingId()),
                        payments.getOrDefault(item.booking().getBookingId(), List.of())))
                .toList();
    }

    private static <T> List<List<T>> batches(List<T> items, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int start = 0; start < items.size(); start += size) {
            result.add(items.subList(start, Math.min(items.size(), start + size)));
        }
        return result;
    }

    @Override
    public OwnerBookingView getBooking(UUID bookingId) throws ServiceException {
        if (bookingId == null) throw new ServiceException("Thiếu mã đơn đặt sân.");
        OwnerBookingRecord record = bookingRepository.findBookingRecordById(bookingId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy đơn đặt sân."));
        return view(record, bookingCancellationRepository.findByBookingId(bookingId).orElse(null),
                paymentQueryPort.findByBookingIds(Set.of(bookingId)));
    }

    private static OwnerBookingView view(OwnerBookingRecord record,
                                         BookingCancellation cancellation,
                                         List<PaymentSnapshot> payments) {
        // Admin chỉ xem: không có chuyển trạng thái nào được phép từ phía admin.
        return new OwnerBookingView(record.booking(), record.venueId(), record.venueName(), record.courtName(),
                cancellation, payments, List.of());
    }
}
