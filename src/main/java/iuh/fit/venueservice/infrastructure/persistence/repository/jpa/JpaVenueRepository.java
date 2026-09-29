package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaVenueRepository extends JpaRepository<VenueEntity, UUID>, JpaSpecificationExecutor<VenueEntity> {
    List<VenueEntity> findByOwnerId(UUID ownerId);

    long countByActiveTrue();
}
