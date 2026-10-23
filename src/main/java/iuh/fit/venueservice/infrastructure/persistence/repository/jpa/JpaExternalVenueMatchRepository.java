package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaExternalVenueMatchRepository extends JpaRepository<ExternalVenueMatchEntity, UUID> {

    long countByExternalVenueId(UUID externalVenueId);

    /** [externalVenueId, số kèo] của mọi sân đã có kèo hoàn tất. */
    @Query("select m.externalVenueId, count(m) from ExternalVenueMatchEntity m group by m.externalVenueId")
    List<Object[]> countByVenue();

    @Modifying
    @Query("""
            delete from ExternalVenueMatchEntity m where m.externalVenueId in
                (select v.externalVenueId from ExternalVenueEntity v where v.region = :region)
            """)
    int deleteByRegion(@Param("region") String region);
}
