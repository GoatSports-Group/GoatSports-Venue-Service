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
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

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

        PageResult<OwnerBookingView> page = service.getBookings(filter);

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

    @Test
    void exportsEveryMatchingBookingWithPaymentsFetchedInBatchesOf100() throws ServiceException {
        List<OwnerBookingRecord> records = IntStream.range(0, 150)
                .mapToObj(index -> record(UUID.randomUUID())).toList();
        AdminBookingFilter filter = new AdminBookingFilter(null, null, null, BookingStatus.COMPLETED, null, null, null, 0, 1);
        when(bookingRepository.findAllBookings(filter.forExport(10_000)))
                .thenReturn(new PageResult<>(records, 150, 0, 10_000, 1));
        when(paymentQueryPort.findByBookingIds(any())).thenReturn(List.of());
        when(bookingCancellationRepository.findByBookingIds(any())).thenReturn(Map.of());

        List<OwnerBookingView> views = service.exportBookings(filter, 10_000);

        assertThat(views).hasSize(150);
        // 150 đơn = 2 lần hỏi payment-service (100 + 50), không bao giờ vượt giới hạn 100 reference ID.
        ArgumentCaptor<Set<UUID>> batches = ArgumentCaptor.forClass(Set.class);
        verify(paymentQueryPort, Mockito.times(2)).findByBookingIds(batches.capture());
        assertThat(batches.getAllValues()).extracting(Set::size).containsExactly(100, 50);
    }

    @Test
    void refusesExportsLargerThanTheLimit() {
        AdminBookingFilter filter = new AdminBookingFilter(null, null, null, null, null, null, null, 0, 1);
        when(bookingRepository.findAllBookings(filter.forExport(10_000)))
                .thenReturn(new PageResult<>(List.of(record()), 10_001, 0, 10_000, 2));

        assertThatThrownBy(() -> service.exportBookings(filter, 10_000))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("10.000");
        verify(paymentQueryPort, never()).findByBookingIds(any());
    }

    private OwnerBookingRecord record() {
        return record(bookingId);
    }

    private OwnerBookingRecord record(UUID bookingId) {
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
