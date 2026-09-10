package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from OwnerApplicationEntity application where application.ownerApplicationId = :id")
    Optional<OwnerApplicationEntity> findByIdForUpdate(@Param("id") UUID id);
}
