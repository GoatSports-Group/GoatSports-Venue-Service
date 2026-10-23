package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

@Repository
public interface JpaOwnerApplicationRepository extends JpaRepository<OwnerApplicationEntity, UUID>,
        JpaSpecificationExecutor<OwnerApplicationEntity>
{
    List<OwnerApplicationEntity> findByUserId(UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, List<OwnerApplicationStatus> statuses);

    long countByClaimedExternalVenueIdAndStatusAndOwnerApplicationIdNot(UUID claimedExternalVenueId,
                                                                         OwnerApplicationStatus status,
                                                                         UUID ownerApplicationId);

    @EntityGraph(attributePaths = { "address", "ownerApplicationDocuments", "venue" })
    @Query("select application from OwnerApplicationEntity application where application.ownerApplicationId = :id")
    Optional<OwnerApplicationEntity> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from OwnerApplicationEntity application where application.ownerApplicationId = :id")
    Optional<OwnerApplicationEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select application.ownerApplicationId
            from OwnerApplicationEntity application
            where exists (
                select document.documentId
                from OwnerApplicationDocumentEntity document
                where document.ownerApplication = application
                  and document.fileUrl like concat(:prefix, '%')
            )
            """)
    List<UUID> findIdsWithDocumentFileUrlPrefix(
            @Param("prefix") String prefix,
            Pageable pageable
    );

    @EntityGraph(attributePaths = { "address", "ownerApplicationDocuments", "venue" })
    @Query("""
            select distinct application
            from OwnerApplicationEntity application
            where application.ownerApplicationId in :ids
            order by application.createdAt asc
            """)
    List<OwnerApplicationEntity> findDetailedByIdIn(@Param("ids") List<UUID> ids);

    List<OwnerApplicationEntity> findAllByUserId(UUID userId);
}
