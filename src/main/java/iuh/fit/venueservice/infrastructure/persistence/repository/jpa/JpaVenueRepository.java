package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.domain.entity.GeoPoint;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaVenueRepository extends JpaRepository<VenueEntity, UUID>, JpaSpecificationExecutor<VenueEntity> {
    // Danh sách / tìm cơ sở: chính sách hủy là @OneToOne(mappedBy), không lazy được — join luôn để khỏi thêm một
    // truy vấn mỗi cơ sở. Sân (collection) và địa chỉ nạp theo lô nhờ hibernate.default_batch_fetch_size.
    @Override
    @EntityGraph(attributePaths = {"address", "cancellationPolicy"})
    Page<VenueEntity> findAll(Specification<VenueEntity> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"address", "cancellationPolicy"})
    List<VenueEntity> findAll(Specification<VenueEntity> spec);

    List<VenueEntity> findByOwnerId(UUID ownerId);

    long countByActiveTrue();

    @Query("select distinct v.ownerId from VenueEntity v where v.ownerId is not null")
    List<UUID> findDistinctOwnerIds();

    boolean existsByOwnerId(UUID ownerId);

    @Query("""
            select new iuh.fit.venueservice.domain.entity.GeoPoint(a.latitude, a.longitude)
            from VenueEntity v join v.address a
            where v.active = true and v.suspendedAt is null
              and a.latitude between :south and :north and a.longitude between :west and :east
            """)
    List<GeoPoint> findActiveLocationsIn(@Param("south") double south, @Param("west") double west,
                                         @Param("north") double north, @Param("east") double east);
}
