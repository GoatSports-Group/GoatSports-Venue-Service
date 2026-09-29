package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.VenueCourtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface JpaVenueCourtRepository extends JpaRepository<VenueCourtEntity, UUID>, JpaSpecificationExecutor<VenueCourtEntity> {
    List<VenueCourtEntity> findByVenue_VenueIdAndDeletedAtIsNull(UUID venueId);

    Optional<VenueCourtEntity> findByVenueCourtIdAndDeletedAtIsNull(UUID venueCourtId);

    long countByActiveTrueAndDeletedAtIsNull();
}
