package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingAutoCompletionJobTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-05T13:00:00Z"), BUSINESS_ZONE
    );

    @Test
    void completesCheckedInBookingWhenCourtTimeEndedAndPaymentSucceeded() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        PaymentQueryPort paymentQueryPort = mock(PaymentQueryPort.class);
        Booking booking = checkedInBooking();
        PaymentSnapshot payment = new PaymentSnapshot(
                UUID.randomUUID(), booking.getBookingId(), "BOOKING_REMAINING", 100_000D,
                "VND", "SUCCEEDED", "MANUAL", "CASH", LocalDateTime.now(CLOCK), null,
                LocalDateTime.now(CLOCK)
        );
        when(bookingRepository.findCompletionCandidatesForUpdate(any(), eq(100)))
                .thenReturn(List.of(booking));
        when(paymentQueryPort.findByBookingIds(Set.of(booking.getBookingId())))
                .thenReturn(List.of(payment));

        new BookingAutoCompletionJob(bookingRepository, paymentQueryPort, CLOCK)
                .completeEligibleBookings();

        verify(bookingRepository).save(booking);
        Assertions.assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void leavesCheckedInBookingOpenWhilePaymentIsOutstanding() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        PaymentQueryPort paymentQueryPort = mock(PaymentQueryPort.class);
        Booking booking = checkedInBooking();
        when(bookingRepository.findCompletionCandidatesForUpdate(any(), eq(100)))
                .thenReturn(List.of(booking));
        when(paymentQueryPort.findByBookingIds(Set.of(booking.getBookingId())))
                .thenReturn(List.of());

        new BookingAutoCompletionJob(bookingRepository, paymentQueryPort, CLOCK)
                .completeEligibleBookings();

        verify(bookingRepository, never()).save(booking);
        Assertions.assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
    }

    private Booking checkedInBooking() {
        return Booking.builder()
                .bookingId(UUID.randomUUID())
                .status(BookingStatus.CHECKED_IN)
                .remainingAmount(100_000D)
                .build();
    }
}
