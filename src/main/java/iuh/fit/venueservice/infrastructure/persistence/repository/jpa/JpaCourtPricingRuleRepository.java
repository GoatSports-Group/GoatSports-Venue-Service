package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.CourtPricingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCourtPricingRuleRepository extends JpaRepository<CourtPricingRuleEntity, UUID> {
    List<CourtPricingRuleEntity> findByCourt_VenueCourtId(UUID courtId);
}
