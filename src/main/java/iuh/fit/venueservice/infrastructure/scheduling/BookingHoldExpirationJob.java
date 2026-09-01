package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "booking.payment-hold-expiration-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BookingHoldExpirationJob {
    private final BookingRepository bookingRepository;
    private final TimeSlotLifecycleService timeSlotLifecycleService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${booking.payment-hold-expiration-scan-ms:30000}")
    @Transactional(rollbackFor = ServiceException.class)
    public void expireStaleHolds() throws ServiceException {
        LocalDateTime now = LocalDateTime.now(clock);
        var expired = bookingRepository.findExpiredPaymentHoldsForUpdate(now);
        for (var booking : expired) {
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    || booking.getHoldExpiresAt() == null
                    || booking.getHoldExpiresAt().isAfter(now)) {
                continue;
            }
            booking.setStatus(BookingStatus.EXPIRED);
            timeSlotLifecycleService.releaseBookingSlot(booking);
            bookingRepository.save(booking);
            log.info("Expired unpaid booking {} and released its time slot", booking.getBookingId());
        }
    }
}
