package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.CourtHold;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourtHoldRepository {
    List<CourtHold> saveAll(List<CourtHold> holds);

    List<CourtHold> findByTournamentId(UUID tournamentId);

    void deleteByTournamentId(UUID tournamentId);

    /** Moi hold cua cac san nay co khoang ngay giao voi [fromDate, toDate]. */
    List<CourtHold> findOverlapping(Collection<UUID> courtIds, LocalDate fromDate, LocalDate toDate);
}
