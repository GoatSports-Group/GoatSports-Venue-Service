package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.port.persistence.TimeSlotRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.TimeSlot;
import iuh.fit.venueservice.domain.enums.TimeSlotStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeSlotLifecycleServiceTest {
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-01T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")
    );

    @Test
    void confirmsLockedSlotAndReleasesFutureBookedSlot() throws Exception {
        UUID courtId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .venueCourtId(courtId).playDate(LocalDate.of(2026, 9, 2))
                .startTime(LocalTime.of(7, 0)).endTime(LocalTime.of(8, 0)).build();
        TimeSlot slot = TimeSlot.builder()
                .venueCourtId(courtId).date(booking.getPlayDate())
                .startTime(booking.getStartTime()).endTime(booking.getEndTime())
                .status(TimeSlotStatus.LOCKED).build();
        TimeSlotRepository repository = mock(TimeSlotRepository.class);
        when(repository.findByCourtAndTimeForUpdate(
                courtId, booking.getPlayDate(), booking.getStartTime(), booking.getEndTime()
        )).thenReturn(Optional.of(slot));
        when(repository.save(slot)).thenReturn(slot);
        TimeSlotLifecycleService lifecycle = new TimeSlotLifecycleService(repository, clock);

        lifecycle.confirmBookingSlot(booking);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.BOOKED);

        lifecycle.releaseBookingSlot(booking);
        assertThat(slot.getStatus()).isEqualTo(TimeSlotStatus.AVAILABLE);
        verify(repository, Mockito.times(2)).save(slot);
    }
}
