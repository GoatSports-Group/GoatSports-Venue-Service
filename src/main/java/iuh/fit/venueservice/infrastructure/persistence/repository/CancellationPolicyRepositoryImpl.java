package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.CancellationPolicyRepository;
import iuh.fit.venueservice.domain.entity.CancellationPolicy;
import iuh.fit.venueservice.infrastructure.persistence.entity.CancellationPolicyEntity;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaCancellationPolicyRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CancellationPolicyRepositoryImpl implements CancellationPolicyRepository {
    private final JpaCancellationPolicyRepository jpaRepository;
    private final JpaVenueRepository venueRepository;

    @Override
    public Optional<CancellationPolicy> findByVenueId(UUID venueId) {
        return jpaRepository.findByVenue_VenueId(venueId).map(this::toDomain);
    }

    @Override
    public CancellationPolicy save(UUID venueId, CancellationPolicy policy) {
        CancellationPolicyEntity entity = jpaRepository.findByVenue_VenueId(venueId)
                .orElseGet(CancellationPolicyEntity::new);
        entity.setVenue(venueRepository.getReferenceById(venueId));
        entity.setFullRefundHoursBefore(policy.getFullRefundHoursBefore());
        entity.setPartialRefundHoursBefore(policy.getPartialRefundHoursBefore());
        entity.setPartialRefundPercentage(policy.getPartialRefundPercentage());
        entity.setNoRefundHoursBefore(policy.getNoRefundHoursBefore());
        return toDomain(jpaRepository.save(entity));
    }

    private CancellationPolicy toDomain(CancellationPolicyEntity entity) {
        return CancellationPolicy.builder()
                .policyId(entity.getPolicyId())
                .fullRefundHoursBefore(entity.getFullRefundHoursBefore())
                .partialRefundHoursBefore(entity.getPartialRefundHoursBefore())
                .partialRefundPercentage(entity.getPartialRefundPercentage())
                .noRefundHoursBefore(entity.getNoRefundHoursBefore())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
