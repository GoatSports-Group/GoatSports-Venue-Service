package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueWantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaExternalVenueWantRepository
        extends JpaRepository<ExternalVenueWantEntity, ExternalVenueWantEntity.Key> {

    long countByIdExternalVenueId(UUID externalVenueId);

    @Query("select w.id.userId from ExternalVenueWantEntity w where w.id.externalVenueId = :externalVenueId")
    List<UUID> findUserIdsByExternalVenueId(@Param("externalVenueId") UUID externalVenueId);

    @Modifying
    @Query("delete from ExternalVenueWantEntity w where w.id.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /** [externalVenueId, số lượt] của mọi sân có người muốn đặt. */
    @Query("select w.id.externalVenueId, count(w) from ExternalVenueWantEntity w group by w.id.externalVenueId")
    List<Object[]> countByVenue();

    @Modifying
    @Query("""
            delete from ExternalVenueWantEntity w where w.id.externalVenueId in
                (select v.externalVenueId from ExternalVenueEntity v where v.region = :region)
            """)
    int deleteByRegion(@Param("region") String region);
}
