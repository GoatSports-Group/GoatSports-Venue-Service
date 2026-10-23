package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueEntity;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaExternalVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ExternalVenueRepositoryImpl implements ExternalVenueRepository {
    private static final int LOOKUP_BATCH = 500;

    private final JpaExternalVenueRepository jpaRepository;

    @Override
    @Transactional
    public int upsertAll(List<ExternalVenue> venues) {
        int saved = 0;
        for (int start = 0; start < venues.size(); start += LOOKUP_BATCH) {
            List<ExternalVenue> batch = venues.subList(start, Math.min(start + LOOKUP_BATCH, venues.size()));
            Map<String, ExternalVenueEntity> existing = jpaRepository.findBySourceAndSourceIdIn(
                            ExternalVenue.SOURCE_OSM, batch.stream().map(ExternalVenue::getSourceId).toList())
                    .stream().collect(Collectors.toMap(ExternalVenueEntity::getSourceId, Function.identity()));
            List<ExternalVenueEntity> entities = new ArrayList<>(batch.size());
            for (ExternalVenue venue : batch) {
                ExternalVenueEntity entity = existing.getOrDefault(venue.getSourceId(), new ExternalVenueEntity());
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
                .build();
    }
}
