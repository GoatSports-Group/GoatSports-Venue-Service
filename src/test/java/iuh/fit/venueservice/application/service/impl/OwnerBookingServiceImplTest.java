package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.request.venue.UpdateOwnerBookingStatusCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.PaymentCollectionPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerBookingServiceImplTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private VenueCourtRepository venueCourtRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private PaymentQueryPort paymentQueryPort;
    @Mock private PaymentCollectionPort paymentCollectionPort;
    @Mock private TimeSlotLifecycleService timeSlotLifecycleService;

    private OwnerBookingServiceImpl service;
    private UUID ownerId;
    private UUID venueId;
    private UUID courtId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        service = new OwnerBookingServiceImpl(
                bookingRepository, venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, timeSlotLifecycleService, Clock.systemDefaultZone()
        );
        ownerId = UUID.randomUUID();
        venueId = UUID.randomUUID();
        courtId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @Test
    void listsOnlyRepositoryScopedBookingsAndUsesRealPaymentSnapshot() throws Exception {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalDate.now().plusDays(1));
        OwnerBookingFilter filter = filter(null, null);
        when(bookingRepository.findOwnedBookings(filter)).thenReturn(
                new PageResult<>(List.of(record(booking)), 1, 0, 20, 1)
        );
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of(
                payment("BOOKING_DEPOSIT", "SUCCEEDED")
        ));

        var result = service.getBookings(filter);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().getFirst().venueId()).isEqualTo(venueId);
        assertThat(result.items().getFirst().payments()).extracting(PaymentSnapshot::status)
                .containsExactly("SUCCEEDED");
        assertThat(result.items().getFirst().allowedTransitions()).isEmpty();
    }

    @Test
    void rejectsCrossOwnerVenueFilterBeforeQueryingBookings() {
        OwnerBookingFilter filter = filter(venueId, null);
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(UUID.randomUUID()).build()
        ));

        assertThatThrownBy(() -> service.getBookings(filter))
                .isInstanceOf(OwnershipException.class);
        verify(bookingRepository, never()).findOwnedBookings(any());
    }

    @Test
    void rejectsCrossOwnerBookingDetail() {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalDate.now().plusDays(1));
        when(bookingRepository.findOwnedBookingById(bookingId, ownerId)).thenReturn(Optional.empty());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getBooking(bookingId, ownerId))
                .isInstanceOf(OwnershipException.class);
        verify(paymentQueryPort, never()).findByBookingIds(any());
    }

    @Test
    void onlyCheckedInPastBookingCanBeCompleted() {
        Booking booking = booking(BookingStatus.CONFIRMED, LocalDate.now().minusDays(1));
        when(bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId))
                .thenReturn(Optional.of(record(booking)));

        assertThatThrownBy(() -> service.updateStatus(new UpdateOwnerBookingStatusCommand(
                bookingId, BookingStatus.COMPLETED, ownerId
        ))).isInstanceOf(ServiceException.class).hasMessageContaining("đã check-in");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void completesCheckedInBookingWithoutChangingPaymentState() throws Exception {
        Booking booking = booking(BookingStatus.CHECKED_IN, LocalDate.now().minusDays(1));
        PaymentSnapshot payment = payment("BOOKING_DEPOSIT", "SUCCEEDED");
        when(bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId))
                .thenReturn(Optional.of(record(booking)));
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of(payment));
        when(bookingRepository.save(booking)).thenReturn(booking);

        var result = service.updateStatus(new UpdateOwnerBookingStatusCommand(
                bookingId, BookingStatus.COMPLETED, ownerId
        ));

        assertThat(result.booking().getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(result.payments()).containsExactly(payment);
        verify(bookingRepository).save(booking);
    }

    @Test
    void paymentFailureDoesNotProduceSuccessOrMutateBooking() {
        Booking booking = booking(BookingStatus.CHECKED_IN, LocalDate.now().minusDays(1));
        when(bookingRepository.findOwnedBookingByIdForUpdate(bookingId, ownerId))
                .thenReturn(Optional.of(record(booking)));
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId)))
                .thenThrow(new IllegalStateException("payment unavailable"));

        assertThatThrownBy(() -> service.updateStatus(new UpdateOwnerBookingStatusCommand(
                bookingId, BookingStatus.COMPLETED, ownerId
        ))).isInstanceOf(IllegalStateException.class).hasMessage("payment unavailable");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        verify(bookingRepository, never()).save(any());
    }

    private OwnerBookingFilter filter(UUID requestedVenueId, UUID requestedCourtId) {
        return new OwnerBookingFilter(
                ownerId, requestedVenueId, requestedCourtId, null, null,
                null, null, 0, 20
        );
    }

    private Booking booking(BookingStatus status, LocalDate playDate) {
        return Booking.builder()
                .bookingId(bookingId).playerId(UUID.randomUUID()).venueCourtId(courtId)
                .playDate(playDate).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .status(status).source(BookingSource.DIRECT).totalPrice(200_000.0)
                .depositAmount(60_000.0).remainingAmount(140_000.0)
                .bookingCode("GS123456").createdAt(LocalDateTime.now().minusDays(2)).build();
    }

    private OwnerBookingRecord record(Booking booking) {
        return new OwnerBookingRecord(booking, venueId, "Goat Arena", "Sân A");
    }

    private PaymentSnapshot payment(String purpose, String status) {
        return new PaymentSnapshot(
                UUID.randomUUID(), bookingId, purpose, 60_000.0, "VND", status,
                LocalDateTime.now().minusDays(1), null, LocalDateTime.now().minusDays(2)
        );
    }
}
