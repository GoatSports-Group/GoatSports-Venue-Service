package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.CourtHoldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCourtHoldRepository extends JpaRepository<CourtHoldEntity, UUID> {
    List<CourtHoldEntity> findByTournamentId(UUID tournamentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CourtHoldEntity hold where hold.tournamentId = :tournamentId")
    void deleteByTournamentId(@Param("tournamentId") UUID tournamentId);

    @Query("""
            select hold from CourtHoldEntity hold
            where hold.venueCourtId in :courtIds
              and hold.fromDate <= :toDate and hold.toDate >= :fromDate
            """)
    List<CourtHoldEntity> findOverlapping(@Param("courtIds") Collection<UUID> courtIds,
                                          @Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate);
}
