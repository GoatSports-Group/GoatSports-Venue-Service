package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueSuggestionRepository;
import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;
import iuh.fit.venueservice.domain.enums.ExternalVenueSuggestionStatus;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueSuggestionEntity;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaExternalVenueSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExternalVenueSuggestionRepositoryImpl implements ExternalVenueSuggestionRepository {
    private final JpaExternalVenueSuggestionRepository jpaRepository;

    @Override
    @Transactional
    public ExternalVenueSuggestion save(ExternalVenueSuggestion suggestion) {
        ExternalVenueSuggestionEntity entity = suggestion.getSuggestionId() == null ? new ExternalVenueSuggestionEntity()
                : jpaRepository.findById(suggestion.getSuggestionId()).orElseGet(ExternalVenueSuggestionEntity::new);
        entity.setSuggestedBy(suggestion.getSuggestedBy());
        entity.setName(suggestion.getName());
        entity.setSportTypes(suggestion.getSportTypes().stream().map(SportType::name).sorted()
                .collect(Collectors.joining(",")));
        entity.setLatitude(suggestion.getLatitude());
        entity.setLongitude(suggestion.getLongitude());
        entity.setAddress(suggestion.getAddress());
        entity.setPhone(suggestion.getPhone());
        entity.setNote(suggestion.getNote());
        entity.setStatus(suggestion.getStatus());
        entity.setRejectReason(suggestion.getRejectReason());
        entity.setExternalVenueId(suggestion.getExternalVenueId());
        entity.setReviewedBy(suggestion.getReviewedBy());
        entity.setReviewedAt(suggestion.getReviewedAt());
        entity.setCreatedAt(suggestion.getCreatedAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalVenueSuggestion> findById(UUID suggestionId) {
        return jpaRepository.findById(suggestionId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExternalVenueSuggestion> findByStatus(ExternalVenueSuggestionStatus status, int page, int size) {
        Page<ExternalVenueSuggestionEntity> result =
                jpaRepository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(), result.getTotalElements(),
                page, size, result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingBy(UUID suggestedBy) {
        return jpaRepository.countBySuggestedByAndStatus(suggestedBy, ExternalVenueSuggestionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(ExternalVenueSuggestionStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    @Transactional
    public void deleteBySuggestedBy(UUID suggestedBy) {
        jpaRepository.deleteBySuggestedBy(suggestedBy);
    }

    private ExternalVenueSuggestion toDomain(ExternalVenueSuggestionEntity entity) {
        Set<SportType> sports = Arrays.stream(entity.getSportTypes().split(","))
                .filter(value -> !value.isBlank())
                .map(SportType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SportType.class)));
        return ExternalVenueSuggestion.builder()
                .suggestionId(entity.getSuggestionId())
                .suggestedBy(entity.getSuggestedBy())
                .name(entity.getName())
                .sportTypes(sports)
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .note(entity.getNote())
                .status(entity.getStatus())
                .rejectReason(entity.getRejectReason())
                .externalVenueId(entity.getExternalVenueId())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
