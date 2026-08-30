package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.CourtPricingRuleRepository;
import iuh.fit.venueservice.domain.entity.CourtPricingRule;
import iuh.fit.venueservice.infrastructure.persistence.entity.CourtPricingRuleEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.CourtPricingRuleEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaCourtPricingRuleRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourtPricingRuleRepositoryImpl implements CourtPricingRuleRepository {
    private final JpaCourtPricingRuleRepository jpaRepository;
    private final JpaVenueCourtRepository venueCourtRepository;
    private final CourtPricingRuleEntityMapper mapper;

    @Override
    public CourtPricingRule save(CourtPricingRule pricingRule) {
        CourtPricingRuleEntity entity;
        if (pricingRule.getPricingRuleId() == null) {
            entity = mapper.createEntity(pricingRule);
        } else {
            entity = jpaRepository.findById(pricingRule.getPricingRuleId())
                    .orElseGet(() -> mapper.createEntity(pricingRule));
            mapper.updateEntity(pricingRule, entity);
        }
        entity.setCourt(venueCourtRepository.getReferenceById(pricingRule.getCourtId()));
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<CourtPricingRule> findById(UUID pricingRuleId) {
        return jpaRepository.findById(pricingRuleId).map(mapper::toDomain);
    }

    @Override
    public List<CourtPricingRule> findByCourtId(UUID venueCourtId) {
        return jpaRepository.findByCourt_VenueCourtIdOrderByDayOfWeekAscStartTimeAsc(venueCourtId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID pricingRuleId) {
        jpaRepository.deleteById(pricingRuleId);
    }
}
