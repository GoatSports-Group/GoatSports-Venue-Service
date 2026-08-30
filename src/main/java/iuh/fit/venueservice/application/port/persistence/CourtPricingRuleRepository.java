package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.domain.entity.CourtPricingRule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtPricingRuleRepository {
    CourtPricingRule save(CourtPricingRule pricingRule);

    Optional<CourtPricingRule> findById(UUID pricingRuleId);

    List<CourtPricingRule> findByCourtId(UUID venueCourtId);

    void deleteById(UUID pricingRuleId);
}
