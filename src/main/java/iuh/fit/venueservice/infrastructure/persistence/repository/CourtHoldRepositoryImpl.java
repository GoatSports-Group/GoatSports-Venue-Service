package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.CourtHoldRepository;
import iuh.fit.venueservice.domain.entity.CourtHold;
import iuh.fit.venueservice.infrastructure.persistence.entity.CourtHoldEntity;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaCourtHoldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CourtHoldRepositoryImpl implements CourtHoldRepository {
    private final JpaCourtHoldRepository jpaRepository;

    @Override
    public List<CourtHold> saveAll(List<CourtHold> holds) {
        return jpaRepository.saveAll(holds.stream().map(this::toEntity).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<CourtHold> findByTournamentId(UUID tournamentId) {
        return jpaRepository.findByTournamentId(tournamentId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteByTournamentId(UUID tournamentId) {
        jpaRepository.deleteByTournamentId(tournamentId);
    }

    @Override
    public List<CourtHold> findOverlapping(Collection<UUID> courtIds, LocalDate fromDate, LocalDate toDate) {
        if (courtIds.isEmpty()) return List.of();
        return jpaRepository.findOverlapping(courtIds, fromDate, toDate).stream().map(this::toDomain).toList();
    }

    private CourtHoldEntity toEntity(CourtHold hold) {
        return CourtHoldEntity.builder().holdId(hold.getHoldId()).tournamentId(hold.getTournamentId())
                .venueId(hold.getVenueId()).venueCourtId(hold.getVenueCourtId())
                .fromDate(hold.getFromDate()).toDate(hold.getToDate())
                .dailyStart(hold.getDailyStart()).dailyEnd(hold.getDailyEnd()).build();
    }

    private CourtHold toDomain(CourtHoldEntity entity) {
        return CourtHold.builder().holdId(entity.getHoldId()).tournamentId(entity.getTournamentId())
                .venueId(entity.getVenueId()).venueCourtId(entity.getVenueCourtId())
                .fromDate(entity.getFromDate()).toDate(entity.getToDate())
                .dailyStart(entity.getDailyStart()).dailyEnd(entity.getDailyEnd()).build();
    }
}
