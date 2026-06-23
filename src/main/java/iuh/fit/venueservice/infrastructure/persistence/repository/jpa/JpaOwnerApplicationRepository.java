package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaOwnerApplicationRepository extends JpaRepository<OwnerApplicationEntity, UUID>,
        JpaSpecificationExecutor<OwnerApplicationEntity>
{
    List<OwnerApplicationEntity> findByUserId(UUID userId);
    boolean existsByUserIdAndStatusIn(UUID userId, List<OwnerApplicationStatus> statuses);
}
