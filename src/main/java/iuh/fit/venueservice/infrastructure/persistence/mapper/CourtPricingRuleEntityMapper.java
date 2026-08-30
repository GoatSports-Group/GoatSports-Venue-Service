package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.CourtPricingRule;
import iuh.fit.venueservice.infrastructure.persistence.entity.CourtPricingRuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourtPricingRuleEntityMapper {
    @Mapping(target = "court", ignore = true)
    CourtPricingRuleEntity createEntity(CourtPricingRule domain);

    @Mapping(target = "court", ignore = true)
    void updateEntity(CourtPricingRule domain, @MappingTarget CourtPricingRuleEntity entity);

    @Mapping(target = "courtId", source = "court.venueCourtId")
    CourtPricingRule toDomain(CourtPricingRuleEntity entity);
}
