package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.OwnerRevenueQuery;
import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.application.dto.response.venue.RevenueBookingRecord;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.PaymentQueryPort;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
class OwnerRevenueServiceImplTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private PaymentQueryPort paymentQueryPort;

    private OwnerRevenueServiceImpl service;
    private UUID ownerId;
    private UUID venueId;

    @BeforeEach
    void setUp() {
        service = new OwnerRevenueServiceImpl(
                bookingRepository, venueRepository, paymentQueryPort
        );
        ownerId = UUID.randomUUID();
        venueId = UUID.randomUUID();
    }

    @Test
    void aggregatesSucceededRevenueAndComparesThePreviousEqualPeriod() throws Exception {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);
        UUID paidBooking = UUID.randomUUID();
        UUID pendingBooking = UUID.randomUUID();
        UUID failedBooking = UUID.randomUUID();
        UUID previousBooking = UUID.randomUUID();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(ownedVenue()));
        when(bookingRepository.findOwnedRevenueBookings(ownerId, venueId, from, to))
                .thenReturn(List.of(
                        booking(paidBooking, from), booking(pendingBooking, from.plusDays(1)),
                        booking(failedBooking, to)
                ));
        when(bookingRepository.findOwnedRevenueBookings(
                ownerId, venueId, LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of(booking(previousBooking, LocalDate.of(2026, 7, 30))));
        when(paymentQueryPort.findByBookingIds(Set.of(
                paidBooking, pendingBooking, failedBooking
        ))).thenReturn(List.of(
                payment(paidBooking, "SUCCEEDED", 60_000),
                payment(paidBooking, "SUCCEEDED", 140_000),
                payment(pendingBooking, "PENDING", 200_000),
                payment(failedBooking, "FAILED", 300_000)
        ));
        when(paymentQueryPort.findByBookingIds(Set.of(previousBooking)))
                .thenReturn(List.of(payment(previousBooking, "SUCCEEDED", 100_000)));

        var result = service.getRevenue(new OwnerRevenueQuery(ownerId, venueId, from, to));

        assertThat(result.periodBasis()).isEqualTo("BOOKING_PLAY_DATE");
        assertThat(result.currentPeriod().bookingCount()).isEqualTo(3);
        assertThat(result.currentPeriod().paidBookingCount()).isEqualTo(1);
        assertThat(result.currentPeriod().totalRevenue()).isEqualByComparingTo("200000.00");
        assertThat(result.previousPeriod().totalRevenue()).isEqualByComparingTo("100000.00");
        assertThat(result.revenueChangePercentage()).isEqualByComparingTo("100.00");
        assertThat(result.bookingCountChangePercentage()).isEqualByComparingTo("200.00");
        assertThat(result.paymentStatusBreakdown())
                .extracting(item -> item.status() + ":" + item.paymentCount())
                .containsExactly("FAILED:1", "PENDING:1", "SUCCEEDED:2");
        assertThat(result.dailyRevenue()).extracting(item -> item.revenue())
                .containsExactly(
                        new BigDecimal("200000.00"), BigDecimal.ZERO.setScale(2),
                        BigDecimal.ZERO.setScale(2)
                );
    }

    @Test
    void rejectsCrossOwnerVenueBeforeReadingBookingsOrPayments() {
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(
                Venue.builder().venueId(venueId).ownerId(UUID.randomUUID()).build()
        ));

        assertThatThrownBy(() -> service.getRevenue(query(venueId)))
                .isInstanceOf(OwnershipException.class);
        verify(bookingRepository, never()).findOwnedRevenueBookings(any(), any(), any(), any());
        verify(paymentQueryPort, never()).findByBookingIds(any());
    }

    @Test
    void emptyScopeReturnsRealZeroesWithoutCallingPaymentService() throws Exception {
        when(bookingRepository.findOwnedRevenueBookings(any(), any(), any(), any()))
                .thenReturn(List.of());

        var result = service.getRevenue(query(null));

        assertThat(result.currentPeriod().bookingCount()).isZero();
        assertThat(result.currentPeriod().totalRevenue()).isEqualByComparingTo("0.00");
        assertThat(result.paymentStatusBreakdown()).isEmpty();
        assertThat(result.dailyRevenue()).hasSize(31)
                .allMatch(point -> point.revenue().compareTo(BigDecimal.ZERO) == 0);
        verify(paymentQueryPort, never()).findByBookingIds(any());
    }

    @Test
    void batchesPaymentQueriesAtTheExistingContractLimit() throws Exception {
        List<RevenueBookingRecord> bookings = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            bookings.add(booking(UUID.randomUUID(), LocalDate.of(2026, 8, 1)));
        }
        when(bookingRepository.findOwnedRevenueBookings(
                ownerId, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)
        )).thenReturn(bookings);
        when(bookingRepository.findOwnedRevenueBookings(
                ownerId, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of());
        when(paymentQueryPort.findByBookingIds(any())).thenReturn(List.of());

        service.getRevenue(query(null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<UUID>> captor = ArgumentCaptor.forClass(Set.class);
        verify(paymentQueryPort, org.mockito.Mockito.times(2)).findByBookingIds(captor.capture());
        assertThat(captor.getAllValues()).extracting(Set::size).containsExactly(100, 1);
    }

    @Test
    void rejectsInvalidRangeAndInvalidCurrencyInsteadOfShowingMisleadingTotals() {
        assertThatThrownBy(() -> service.getRevenue(new OwnerRevenueQuery(
                ownerId, null, LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1)
        ))).isInstanceOf(ServiceException.class);

        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findOwnedRevenueBookings(
                ownerId, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)
        )).thenReturn(List.of(booking(bookingId, LocalDate.of(2026, 8, 1))));
        when(paymentQueryPort.findByBookingIds(Set.of(bookingId)))
                .thenReturn(List.of(payment(bookingId, "SUCCEEDED", 10, "USD")));

        assertThatThrownBy(() -> service.getRevenue(query(null)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("tiền tệ");
    }

    private OwnerRevenueQuery query(UUID requestedVenueId) {
        return new OwnerRevenueQuery(
                ownerId, requestedVenueId,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)
        );
    }

    private Venue ownedVenue() {
        return Venue.builder().venueId(venueId).ownerId(ownerId).build();
    }

    private RevenueBookingRecord booking(UUID bookingId, LocalDate playDate) {
        return new RevenueBookingRecord(bookingId, playDate);
    }

    private PaymentSnapshot payment(UUID bookingId, String status, double amount) {
        return payment(bookingId, status, amount, "VND");
    }

    private PaymentSnapshot payment(
            UUID bookingId, String status, double amount, String currency
    ) {
        return new PaymentSnapshot(
                UUID.randomUUID(), bookingId, "BOOKING_DEPOSIT", amount, currency, status,
                LocalDateTime.of(2026, 7, 30, 10, 0), null,
                LocalDateTime.of(2026, 7, 29, 10, 0)
        );
    }
}
