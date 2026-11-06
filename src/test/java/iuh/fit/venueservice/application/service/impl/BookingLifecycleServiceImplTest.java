package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingLifecyclePort;
import iuh.fit.venueservice.application.port.BookingLifecyclePort.StartBookingLifecycle;
import iuh.fit.venueservice.application.port.BookingNotificationPort;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingLifecycleServiceImplTest {
    /** 20:00 ngày 05/09/2026 giờ Việt Nam. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-05T13:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final LocalDate TODAY = NOW.toLocalDate();

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PaymentQueryPort payments = mock(PaymentQueryPort.class);
    private final TimeSlotLifecycleService slots = mock(TimeSlotLifecycleService.class);
    private final BookingNotificationPort notifications = mock(BookingNotificationPort.class);
    private final BookingLifecyclePort lifecycle = mock(BookingLifecyclePort.class);
    private final BookingLifecycleServiceImpl service =
            new BookingLifecycleServiceImpl(bookings, payments, slots, notifications, lifecycle, CLOCK);

    @Test
    void expiresUnpaidHoldAndReleasesItsSlot() throws Exception {
        Booking booking = stored(booking(BookingStatus.PENDING_PAYMENT, LocalTime.of(21, 0), LocalTime.of(22, 0)));
        booking.setHoldExpiresAt(NOW.minusSeconds(1));

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.EXPIRED)).isEqualTo(BookingStatus.EXPIRED);
        verify(slots).releaseBookingSlot(booking);
        verify(bookings).save(booking);
    }

    @Test
    void paidBookingIsNotExpiredAndTheProcessLearnsItIsConfirmed() throws Exception {
        Booking booking = stored(booking(BookingStatus.CONFIRMED, LocalTime.of(21, 0), LocalTime.of(22, 0)));
        booking.setHoldExpiresAt(NOW.minusMinutes(1));

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.EXPIRED)).isEqualTo(BookingStatus.CONFIRMED);
        verify(slots, never()).releaseBookingSlot(any());
        verify(bookings, never()).save(any());
    }

    @Test
    void neverExpiresWalkInEvenWhenLegacyHoldIsOverdue() throws Exception {
        Booking booking = stored(booking(BookingStatus.PENDING_PAYMENT, LocalTime.of(21, 0), LocalTime.of(22, 0)));
        booking.setSource(BookingSource.WALK_IN);
        booking.setHoldExpiresAt(NOW.minusMinutes(1));

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.EXPIRED))
                .isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(slots, never()).releaseBookingSlot(any());
    }

    @Test
    void confirmedBookingPastItsEndWithoutCheckInBecomesNoShowAndThePlayerIsTold() throws Exception {
        Booking booking = stored(booking(BookingStatus.CONFIRMED, LocalTime.of(18, 0), LocalTime.of(19, 0)));

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.NO_SHOW)).isEqualTo(BookingStatus.NO_SHOW);
        verify(bookings).save(booking);
        verify(notifications).notifyPayer(eq(booking.getPlayerId()), eq(booking.getBookingId()),
                eq("booking-no-show:" + booking.getBookingId()), eq("Không đến nhận sân"),
                contains("tiền cọc không được hoàn"));
    }

    @Test
    void checkedInBookingIsNeverMarkedNoShow() throws Exception {
        Booking booking = stored(booking(BookingStatus.CHECKED_IN, LocalTime.of(18, 0), LocalTime.of(19, 0)));

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.NO_SHOW))
                .isEqualTo(BookingStatus.CHECKED_IN);
        verify(notifications, never()).notifyPayer(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void bookingStillBeingPlayedIsLeftAlone() throws Exception {
        Booking booking = stored(booking(BookingStatus.CONFIRMED, LocalTime.of(19, 30), LocalTime.of(21, 0)));

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.NO_SHOW))
                .isEqualTo(BookingStatus.CONFIRMED);
        verify(bookings, never()).save(any());
    }

    @Test
    void completesCheckedInBookingWhenPlayEndedAndRemainingIsPaid() throws Exception {
        Booking booking = stored(booking(BookingStatus.CHECKED_IN, LocalTime.of(18, 0), LocalTime.of(19, 0)));
        when(payments.findByBookingIds(Set.of(booking.getBookingId()))).thenReturn(List.of(new PaymentSnapshot(
                UUID.randomUUID(), booking.getBookingId(), "BOOKING_REMAINING", 140_000D, "VND", "SUCCEEDED",
                "MANUAL", "CASH", NOW, null, NOW)));

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.COMPLETED))
                .isEqualTo(BookingStatus.COMPLETED);
        verify(bookings).save(booking);
    }

    @Test
    void leavesCheckedInBookingOpenWhileRemainingIsOutstanding() throws Exception {
        Booking booking = stored(booking(BookingStatus.CHECKED_IN, LocalTime.of(18, 0), LocalTime.of(19, 0)));
        when(payments.findByBookingIds(Set.of(booking.getBookingId()))).thenReturn(List.of());

        assertThat(service.applyTimedState(booking.getBookingId(), BookingStatus.COMPLETED))
                .isEqualTo(BookingStatus.CHECKED_IN);
        verify(bookings, never()).save(any());
    }

    @Test
    void missingBookingIsReportedAsNotFound() {
        UUID bookingId = UUID.randomUUID();
        when(bookings.findByIdForUpdate(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyTimedState(bookingId, BookingStatus.EXPIRED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adoptionOpensTheProcessAtTheCurrentStepAndStoresItsKey() throws Exception {
        Booking booking = stored(booking(BookingStatus.CONFIRMED, LocalTime.of(21, 0), LocalTime.of(22, 0)));
        booking.setRemainingPaymentId(UUID.randomUUID());
        when(lifecycle.start(any())).thenReturn(42L);

        service.adoptIntoLifecycle(booking.getBookingId());

        ArgumentCaptor<StartBookingLifecycle> command = ArgumentCaptor.forClass(StartBookingLifecycle.class);
        verify(lifecycle).start(command.capture());
        assertThat(command.getValue().status()).isEqualTo("CONFIRMED");
        assertThat(command.getValue().remainingPaid()).isTrue();
        assertThat(command.getValue().holdExpiresAt()).isNull();
        assertThat(command.getValue().playEndAt()).isEqualTo("2026-09-05T22:00+07:00");
        assertThat(booking.getLifecycleInstanceKey()).isEqualTo(42L);
        verify(bookings).save(booking);
    }

    @Test
    void adoptionSkipsBookingsThatAlreadyHaveAProcessOrHaveEnded() throws Exception {
        Booking tracked = stored(booking(BookingStatus.CONFIRMED, LocalTime.of(21, 0), LocalTime.of(22, 0)));
        tracked.setLifecycleInstanceKey(7L);
        Booking ended = stored(booking(BookingStatus.CONFIRMED, LocalTime.of(18, 0), LocalTime.of(19, 0)));

        service.adoptIntoLifecycle(tracked.getBookingId());
        service.adoptIntoLifecycle(ended.getBookingId());

        verify(lifecycle, never()).start(any());
    }

    @Test
    void adoptionFailsWhileWorkflowServiceIsUnavailableSoTheJobRetriesLater() {
        Booking booking = stored(booking(BookingStatus.PENDING_PAYMENT, LocalTime.of(21, 0), LocalTime.of(22, 0)));
        when(lifecycle.start(any())).thenReturn(null);

        assertThatThrownBy(() -> service.adoptIntoLifecycle(booking.getBookingId()))
                .isInstanceOf(ServiceException.class);
        assertThat(booking.getLifecycleInstanceKey()).isNull();
    }

    private Booking stored(Booking booking) {
        when(bookings.findByIdForUpdate(booking.getBookingId())).thenReturn(Optional.of(booking));
        return booking;
    }

    private static Booking booking(BookingStatus status, LocalTime start, LocalTime end) {
        return Booking.builder().bookingId(UUID.randomUUID()).playerId(UUID.randomUUID()).bookingCode("GSTEST01")
                .source(BookingSource.DIRECT).status(status).playDate(TODAY).startTime(start).endTime(end)
                .remainingAmount(140_000D).build();
    }
}
