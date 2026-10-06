package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.response.stats.PlatformBookingStats;
import iuh.fit.venueservice.application.port.persistence.PlatformStatsRepository;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlatformStatsRepositoryImpl implements PlatformStatsRepository {
    private final JpaVenueRepository venueRepository;
    private final JpaVenueCourtRepository courtRepository;
    private final JpaBookingRepository bookingRepository;

    @Override
    public long countActiveVenues() {
        return venueRepository.countByActiveTrue();
    }

    @Override
    public long countActiveCourts() {
        return courtRepository.countByActiveTrueAndDeletedAtIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingRow> bookingRows(LocalDateTime from, LocalDateTime to) {
        return bookingRepository.findStatRows(from, to).stream()
                .map(row -> new BookingRow((LocalDateTime) row[0], (BookingStatus) row[1],
                        row[2] == null ? null : ((Number) row[2]).doubleValue(),
                        row[3] == null ? null : ((Number) row[3]).doubleValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformBookingStats.VenueStat> topVenues(LocalDateTime from, LocalDateTime to,
                                                          Collection<BookingStatus> paidStatuses, int limit) {
        return bookingRepository.findTopVenues(from, to, paidStatuses, BookingStatus.NO_SHOW, PageRequest.of(0, limit)).stream()
                .map(row -> new PlatformBookingStats.VenueStat((UUID) row[0], (String) row[1],
                        ((Number) row[2]).longValue(), row[3] == null ? 0D : ((Number) row[3]).doubleValue()))
                .toList();
    }
}
