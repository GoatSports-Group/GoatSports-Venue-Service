package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.VenueCourtEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaVenueCourtRepository extends JpaRepository<VenueCourtEntity, UUID>, JpaSpecificationExecutor<VenueCourtEntity> {
}
