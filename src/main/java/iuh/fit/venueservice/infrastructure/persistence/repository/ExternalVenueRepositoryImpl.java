package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueMatchEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueWantEntity;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaExternalVenueMatchRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaExternalVenueRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaExternalVenueWantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExternalVenueRepositoryImpl implements ExternalVenueRepository {
    private static final int LOOKUP_BATCH = 500;

    private final JpaExternalVenueRepository jpaRepository;
    private final JpaExternalVenueWantRepository wantRepository;
    private final JpaExternalVenueMatchRepository matchRepository;

    @Override
    @Transactional
    public int upsertAll(List<ExternalVenue> venues) {
        int saved = 0;
        for (int start = 0; start < venues.size(); start += LOOKUP_BATCH) {
            List<ExternalVenue> batch = venues.subList(start, Math.min(start + LOOKUP_BATCH, venues.size()));
            Map<String, ExternalVenueEntity> existing = new HashMap<>();
            batch.stream().collect(Collectors.groupingBy(ExternalVenue::getSource)).forEach((source, sameSource) ->
                    jpaRepository.findBySourceAndSourceIdIn(source, sameSource.stream().map(ExternalVenue::getSourceId).toList())
                            .forEach(entity -> existing.put(source + "/" + entity.getSourceId(), entity)));
            List<ExternalVenueEntity> entities = new ArrayList<>(batch.size());
            for (ExternalVenue venue : batch) {
                ExternalVenueEntity entity = existing.getOrDefault(venue.getSource() + "/" + venue.getSourceId(),
                        new ExternalVenueEntity());
                copy(venue, entity);
                entities.add(entity);
            }
            saved += jpaRepository.saveAll(entities).size();
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalVenue> findInBounds(GeoBounds bounds, SportType sportType, String keyword, int limit) {
        // Chuỗi rỗng thay cho "không lọc": like '%%' khớp mọi dòng, tránh tham số null mà PostgreSQL không đoán được kiểu.
        String sport = sportType == null ? "" : sportType.name();
        String term = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return jpaRepository.findInBounds(bounds.south(), bounds.west(), bounds.north(), bounds.east(), sport, term,
                        PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExternalVenue> findById(UUID externalVenueId) {
        return jpaRepository.findById(externalVenueId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalVenueRegionSummary> summarizeByRegion() {
        return jpaRepository.summarizeByRegion();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExternalVenue> findByRegion(String region, int page, int size) {
        Page<ExternalVenueEntity> result = jpaRepository.findByRegionOrderByName(region, PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(), result.getTotalElements(),
                page, size, result.getTotalPages());
    }

    @Override
    @Transactional
    public ExternalVenue save(ExternalVenue venue) {
        ExternalVenueEntity entity = venue.getExternalVenueId() == null ? new ExternalVenueEntity()
                : jpaRepository.findById(venue.getExternalVenueId()).orElseGet(ExternalVenueEntity::new);
        copy(venue, entity);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public int deleteByRegion(String region) {
        wantRepository.deleteByRegion(region);
        matchRepository.deleteByRegion(region);
        return jpaRepository.deleteByRegion(region);
    }

    @Override
    @Transactional
    public void addWant(UUID externalVenueId, UUID userId) {
        ExternalVenueWantEntity.Key key = new ExternalVenueWantEntity.Key(externalVenueId, userId);
        if (!wantRepository.existsById(key)) {
            wantRepository.save(new ExternalVenueWantEntity(key, LocalDateTime.now()));
        }
    }

    @Override
    @Transactional
    public void removeWant(UUID externalVenueId, UUID userId) {
        wantRepository.deleteById(new ExternalVenueWantEntity.Key(externalVenueId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public long countWants(UUID externalVenueId) {
        return wantRepository.countByIdExternalVenueId(externalVenueId);
    }

    @Override
    @Transactional
    public Optional<ExternalVenue> recordInvite(UUID externalVenueId) {
        return jpaRepository.findById(externalVenueId).map(entity -> {
            entity.setInviteCount(entity.getInviteCount() + 1);
            entity.setLastInvitedAt(LocalDateTime.now());
            return toDomain(jpaRepository.save(entity));
        });
    }

    @Override
    @Transactional
    public boolean markClaimed(UUID externalVenueId, UUID venueId) {
        ExternalVenueEntity entity = jpaRepository.findById(externalVenueId).orElse(null);
        if (entity == null) return false;
        if (entity.getClaimedVenueId() != null) return entity.getClaimedVenueId().equals(venueId);
        entity.setClaimedVenueId(venueId);
        entity.setClaimedAt(LocalDateTime.now());
        jpaRepository.save(entity);
        return true;
    }

    @Override
    @Transactional
    public Optional<UUID> claimWantersNotification(UUID venueId) {
        return jpaRepository.findByClaimedVenueId(venueId)
                .filter(entity -> entity.getWantersNotifiedAt() == null)
                .map(entity -> {
                    entity.setWantersNotifiedAt(LocalDateTime.now());
                    jpaRepository.save(entity);
                    return entity.getExternalVenueId();
                });
    }

    @Override
    @Transactional
    public void releaseWantersNotification(UUID externalVenueId) {
        jpaRepository.findById(externalVenueId).ifPresent(entity -> {
            entity.setWantersNotifiedAt(null);
            jpaRepository.save(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findWanterIds(UUID externalVenueId) {
        return wantRepository.findUserIdsByExternalVenueId(externalVenueId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMatches(UUID externalVenueId) {
        return matchRepository.countByExternalVenueId(externalVenueId);
    }

    @Override
    @Transactional
    public void removeWantsBy(UUID userId) {
        wantRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasWant(UUID externalVenueId, UUID userId) {
        return wantRepository.existsById(new ExternalVenueWantEntity.Key(externalVenueId, userId));
    }

    @Override
    @Transactional
    public void recordMatch(UUID sessionId, UUID externalVenueId) {
        if (!matchRepository.existsById(sessionId) && jpaRepository.existsById(externalVenueId)) {
            matchRepository.save(new ExternalVenueMatchEntity(sessionId, externalVenueId, LocalDateTime.now()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalVenueDemand> findDemand() {
        Map<UUID, Long> wants = counts(wantRepository.countByVenue());
        Map<UUID, Long> matches = counts(matchRepository.countByVenue());
        Set<UUID> ids = new HashSet<>(wants.keySet());
        ids.addAll(matches.keySet());
        return jpaRepository.findAllById(ids).stream()
                .filter(entity -> entity.getClaimedVenueId() == null)
                .map(entity -> new ExternalVenueDemand(toDomain(entity),
                        wants.getOrDefault(entity.getExternalVenueId(), 0L),
                        matches.getOrDefault(entity.getExternalVenueId(), 0L)))
                .toList();
    }

    private static Map<UUID, Long> counts(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    private void copy(ExternalVenue venue, ExternalVenueEntity entity) {
        entity.setSource(venue.getSource());
        entity.setSourceId(venue.getSourceId());
        entity.setName(venue.getName());
        entity.setSportTypes(venue.getSportTypes().stream().map(SportType::name).sorted()
                .collect(Collectors.joining(",")));
        entity.setLatitude(venue.getLatitude());
        entity.setLongitude(venue.getLongitude());
        entity.setAddress(venue.getAddress());
        entity.setPhone(venue.getPhone());
        entity.setWebsite(venue.getWebsite());
        entity.setOpeningHours(venue.getOpeningHours());
        entity.setRegion(venue.getRegion());
        entity.setImportedAt(venue.getImportedAt());
    }

    private ExternalVenue toDomain(ExternalVenueEntity entity) {
        Set<SportType> sports = Arrays.stream(entity.getSportTypes().split(","))
                .filter(value -> !value.isBlank())
                .map(SportType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SportType.class)));
        return ExternalVenue.builder()
                .externalVenueId(entity.getExternalVenueId())
                .source(entity.getSource())
                .sourceId(entity.getSourceId())
                .name(entity.getName())
                .sportTypes(sports)
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .website(entity.getWebsite())
                .openingHours(entity.getOpeningHours())
                .region(entity.getRegion())
                .importedAt(entity.getImportedAt())
                .inviteCount(entity.getInviteCount())
                .lastInvitedAt(entity.getLastInvitedAt())
                .claimedVenueId(entity.getClaimedVenueId())
                .claimedAt(entity.getClaimedAt())
                .build();
    }
}
