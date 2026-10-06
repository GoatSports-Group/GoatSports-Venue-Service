package iuh.fit.venueservice.infrastructure.scheduling;

import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingNoShowJobTest {
    /** 20:00 ngay 05/09/2026 gio Viet Nam. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-05T13:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Test
    void confirmedBookingPastItsEndWithoutCheckInBecomesNoShowAndThePlayerIsTold() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingNotificationPort notifications = mock(BookingNotificationPort.class);
        Booking booking = confirmed(LocalDate.of(2026, 9, 5), LocalTime.of(18, 0), LocalTime.of(19, 0));
        when(bookings.findNoShowCandidatesForUpdate(any(), eq(100))).thenReturn(List.of(booking));

        new BookingNoShowJob(bookings, notifications, CLOCK).markNoShows();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        verify(bookings).save(booking);
        verify(notifications).notifyPayer(eq(booking.getPlayerId()), eq(booking.getBookingId()),
                eq("booking-no-show:" + booking.getBookingId()), eq("Không đến nhận sân"), contains("tiền cọc không được hoàn"));
    }

    @Test
    void bookingStillBeingPlayedOrAlreadyCheckedInIsLeftAlone() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingNotificationPort notifications = mock(BookingNotificationPort.class);
        Booking playing = confirmed(LocalDate.of(2026, 9, 5), LocalTime.of(19, 30), LocalTime.of(21, 0));
        Booking checkedIn = confirmed(LocalDate.of(2026, 9, 4), LocalTime.of(18, 0), LocalTime.of(19, 0));
        checkedIn.setStatus(BookingStatus.CHECKED_IN);
        when(bookings.findNoShowCandidatesForUpdate(any(), eq(100))).thenReturn(List.of(playing, checkedIn));

        new BookingNoShowJob(bookings, notifications, CLOCK).markNoShows();

        assertThat(playing.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(checkedIn.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        verify(bookings, never()).save(any());
        verify(notifications, never()).notifyPayer(any(), any(), anyString(), anyString(), anyString());
    }

    private static Booking confirmed(LocalDate day, LocalTime start, LocalTime end) {
        return Booking.builder().bookingId(UUID.randomUUID()).playerId(UUID.randomUUID()).bookingCode("GSTEST01")
                .status(BookingStatus.CONFIRMED).playDate(day).startTime(start).endTime(end).build();
    }
}
