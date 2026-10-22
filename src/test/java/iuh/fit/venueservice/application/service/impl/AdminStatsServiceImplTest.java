package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.stats.PlatformBookingStats;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.PlatformStatsRepository;
import iuh.fit.venueservice.application.port.persistence.PlatformStatsRepository.BookingRow;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceImplTest {
    @Mock private PlatformStatsRepository statsRepository;

    @Test
    void countsEveryStatusButOnlyPaidBookingsIntoGmvAndFillsEmptyDays() throws ServiceException {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 3);
        when(statsRepository.bookingRows(from.atStartOfDay(), to.plusDays(1).atStartOfDay())).thenReturn(List.of(
                new BookingRow(from.atTime(9, 0), BookingStatus.CONFIRMED, 200_000.0, 60_000.0),
                new BookingRow(from.atTime(10, 0), BookingStatus.CANCELLED, 150_000.0, 45_000.0),
                new BookingRow(to.atTime(18, 0), BookingStatus.COMPLETED, 300_000.0, 90_000.0),
                new BookingRow(to.atTime(19, 0), BookingStatus.PENDING_PAYMENT, 120_000.0, 36_000.0),
                // Khong den nhan san: chi tinh tien coc co so da giu.
                new BookingRow(to.atTime(20, 0), BookingStatus.NO_SHOW, 400_000.0, 120_000.0)));
        when(statsRepository.countActiveVenues()).thenReturn(4L);
        when(statsRepository.countActiveCourts()).thenReturn(17L);
        when(statsRepository.topVenues(any(), any(), eq(AdminStatsServiceImpl.PAID), anyInt())).thenReturn(List.of());

        PlatformBookingStats stats = new AdminStatsServiceImpl(statsRepository).getBookingStats(from, to);

        assertThat(stats.bookings()).isEqualTo(5);
        assertThat(stats.gmv()).isEqualTo(620_000.0);
        assertThat(stats.bookingsByStatus()).containsEntry("CANCELLED", 1L).containsEntry("REFUNDED", 0L);
        assertThat(stats.byDay()).extracting(PlatformBookingStats.DayStat::bookings).containsExactly(2L, 0L, 3L);
        assertThat(stats.byDay()).extracting(PlatformBookingStats.DayStat::gmv).containsExactly(200_000.0, 0.0, 420_000.0);
        assertThat(stats.activeVenues()).isEqualTo(4);
    }

    @Test
    void rejectsReversedOrOverlongRanges() {
        AdminStatsServiceImpl service = new AdminStatsServiceImpl(statsRepository);
        assertThatThrownBy(() -> service.getBookingStats(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.getBookingStats(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(ServiceException.class);
    }
}
