package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.CourtHold;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourtHoldRepository {
    List<CourtHold> saveAll(List<CourtHold> holds);

    List<CourtHold> findByTournamentId(UUID tournamentId);

    void deleteByTournamentId(UUID tournamentId);

    /** Moi hold cua cac san nay co khoang ngay giao voi [fromDate, toDate]. */
    List<CourtHold> findOverlapping(Collection<UUID> courtIds, LocalDate fromDate, LocalDate toDate);

    /** Khung gio [start, end) cua san nay trong ngay co nam trong lich giu san cua giai nao khong. */
    default boolean isHeld(UUID courtId, LocalDate date, LocalTime start, LocalTime end) {
        return findOverlapping(List.of(courtId), date, date).stream().anyMatch(hold -> hold.covers(date, start, end));
    }
}
