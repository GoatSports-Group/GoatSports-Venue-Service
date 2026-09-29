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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {
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
