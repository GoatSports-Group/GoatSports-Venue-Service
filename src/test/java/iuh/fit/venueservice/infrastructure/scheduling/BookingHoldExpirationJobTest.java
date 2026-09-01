package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingHoldExpirationJobTest {
    @Test
    void expiresPendingBookingAndReleasesItsSlot() throws Exception {
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-01T01:15:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")
        );
        LocalDateTime now = LocalDateTime.now(clock);
        Booking booking = Booking.builder()
                .bookingId(UUID.randomUUID())
                .status(BookingStatus.PENDING_PAYMENT)
                .holdExpiresAt(now.minusSeconds(1))
                .build();
        BookingRepository bookingRepository = mock(BookingRepository.class);
        TimeSlotLifecycleService lifecycle = mock(TimeSlotLifecycleService.class);
        when(bookingRepository.findExpiredPaymentHoldsForUpdate(now)).thenReturn(List.of(booking));

        new BookingHoldExpirationJob(bookingRepository, lifecycle, clock).expireStaleHolds();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        verify(lifecycle).releaseBookingSlot(booking);
        verify(bookingRepository).save(booking);
    }
}
