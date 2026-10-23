package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaExternalVenueRepository extends JpaRepository<ExternalVenueEntity, UUID> {

    List<ExternalVenueEntity> findBySourceAndSourceIdIn(String source, Collection<String> sourceIds);

    @Query("""
            select v from ExternalVenueEntity v
            where v.latitude between :south and :north and v.longitude between :west and :east
              and v.sportTypes like concat('%', :sport, '%')
              and lower(v.name) like concat('%', :keyword, '%')
            order by v.name
            """)
    List<ExternalVenueEntity> findInBounds(@Param("south") double south, @Param("west") double west,
                                           @Param("north") double north, @Param("east") double east,
                                           @Param("sport") String sport, @Param("keyword") String keyword,
                                           Pageable pageable);

    @Query("""
            select new iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary(
                v.region, count(v), max(v.importedAt))
            from ExternalVenueEntity v group by v.region order by v.region
            """)
    List<ExternalVenueRegionSummary> summarizeByRegion();

    Page<ExternalVenueEntity> findByRegionOrderByName(String region, Pageable pageable);
}
