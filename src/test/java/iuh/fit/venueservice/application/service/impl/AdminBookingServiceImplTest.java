package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.AdminBookingFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.entity.Booking;
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
import java.util.List;
import java.util.Map;
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
class AdminBookingServiceImplTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingCancellationRepository bookingCancellationRepository;
    @Mock private PaymentQueryPort paymentQueryPort;

    private AdminBookingServiceImpl service;
    private final UUID bookingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AdminBookingServiceImpl(bookingRepository, bookingCancellationRepository, paymentQueryPort);
    }

    @Test
    void listsBookingsOfEveryVenueWithTheirPaymentsAndNoOwnerActions() throws ServiceException {
        AdminBookingFilter filter = new AdminBookingFilter(null, null, null, BookingStatus.CONFIRMED, "arena", null, null, 0, 50);
        when(bookingRepository.findAllBookings(filter)).thenReturn(new PageResult<>(List.of(record()), 1, 0, 50, 1));
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of(payment()));
        when(bookingCancellationRepository.findByBookingIds(Set.of(bookingId))).thenReturn(Map.of());

        var page = service.getBookings(filter);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(view -> {
            assertThat(view.venueName()).isEqualTo("Goat Arena");
            assertThat(view.payments()).hasSize(1);
            assertThat(view.allowedTransitions()).isEmpty();
        });
    }

    @Test
    void rejectsOversizedPagesBeforeQuerying() {
        assertThatThrownBy(() -> service.getBookings(new AdminBookingFilter(null, null, null, null, null, null, null, 0, 200)))
                .isInstanceOf(ServiceException.class);
        verify(bookingRepository, never()).findAllBookings(any());
    }

    @Test
    void readsAnySingleBookingRegardlessOfOwner() throws ServiceException {
        when(bookingRepository.findBookingRecordById(bookingId)).thenReturn(Optional.of(record()));
        when(bookingCancellationRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId))).thenReturn(List.of());

        assertThat(service.getBooking(bookingId).booking().getBookingCode()).isEqualTo("GS123456");
    }

    private OwnerBookingRecord record() {
        Booking booking = Booking.builder()
                .bookingId(bookingId).playerId(UUID.randomUUID()).venueCourtId(UUID.randomUUID())
                .playDate(LocalDate.now()).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .status(BookingStatus.CONFIRMED).source(BookingSource.DIRECT).totalPrice(200_000.0)
                .depositAmount(60_000.0).remainingAmount(140_000.0)
                .bookingCode("GS123456").createdAt(LocalDateTime.now()).build();
        return new OwnerBookingRecord(booking, UUID.randomUUID(), "Goat Arena", "Sân A");
    }

    private PaymentSnapshot payment() {
        return new PaymentSnapshot(UUID.randomUUID(), bookingId, "BOOKING_DEPOSIT", 60_000.0, "VND", "SUCCEEDED",
                LocalDateTime.now(), null, LocalDateTime.now());
    }
}
