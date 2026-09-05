package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static iuh.fit.venueservice.application.service.BookingPaymentStatusPolicy.isFullyPaid;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "booking.auto-completion-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BookingAutoCompletionJob {
    private final BookingRepository bookingRepository;
    private final PaymentQueryPort paymentQueryPort;
    private final Clock clock;

    @Value("${booking.auto-completion-batch-size:100}")
    private int batchSize = 100;

    @Scheduled(fixedDelayString = "${booking.auto-completion-scan-ms:60000}")
    @Transactional
    public void completeEligibleBookings() {
        LocalDateTime now = LocalDateTime.now(clock);
        var candidates = bookingRepository.findCompletionCandidatesForUpdate(now, batchSize);
        if (candidates.isEmpty()) return;

        Set<UUID> bookingIds = candidates.stream()
                .map(booking -> booking.getBookingId())
                .collect(Collectors.toSet());
        Map<UUID, List<PaymentSnapshot>> paymentsByBooking = paymentQueryPort.findByBookingIds(bookingIds).stream()
                .collect(Collectors.groupingBy(PaymentSnapshot::referenceId));

        int completed = 0;
        for (var booking : candidates) {
            if (booking.getStatus() != BookingStatus.CHECKED_IN
                    || !isFullyPaid(booking, paymentsByBooking.getOrDefault(
                            booking.getBookingId(), Collections.emptyList()))) {
                continue;
            }
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);
            completed++;
        }
        if (completed > 0) {
            log.info("Automatically completed {} eligible bookings", completed);
        }
    }
}
