package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.AdminVenueFilter;
import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.SportType;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.AddressEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VenueRepositoryImpl implements VenueRepository {
    private final JpaVenueRepository jpaRepository;
    private final VenueEntityMapper venueEntityMapper;
    private final AddressEntityMapper addressEntityMapper;

    @Override
    public Venue save(Venue venue) {
        VenueEntity entity;
        if (venue.getVenueId() == null) {
            entity = venueEntityMapper.createEntity(venue);
        } else {
            entity = jpaRepository.findById(venue.getVenueId())
                    .orElseGet(() -> venueEntityMapper.createEntity(venue));
            venueEntityMapper.updateEntity(venue, entity, addressEntityMapper);
        }
        var saved = jpaRepository.save(entity);
        return venueEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Venue> findById(UUID id) {
        return jpaRepository.findById(id).map(venueEntityMapper::toDomain);
    }

    @Override
    public List<Venue> findByUserId(UUID userId) {
        return jpaRepository.findByOwnerId(userId).stream()
                .map(venueEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UUID> findOwnerIds() {
        return jpaRepository.findDistinctOwnerIds();
    }

    @Override
    public PageResult<Venue> search(SearchVenueCommand command) {
        Specification<VenueEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (command.sportType() != null) {
                var court = root.join("venueCourts", JoinType.INNER);
                predicates.add(cb.isTrue(court.get("active")));
                predicates.add(cb.equal(court.get("sportType"), command.sportType()));
                query.distinct(true);
            }

            if (command.usesSemanticSearch()) {
                // Semantic THAY THE keyword LIKE chu khong AND them: neu AND thi ket qua
                // bi thu ve dung tap ma LIKE tim duoc, tuc mat sach gia tri cua semantic.
                List<UUID> semanticIds = command.semanticVenueIds();
                if (semanticIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("venueId").in(semanticIds));
                }
            } else if (command.keyword() != null && !command.keyword().trim().isEmpty()) {
                String kw = "%" + command.keyword().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), kw);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), kw);
                var address = root.join("address", JoinType.INNER);
                Predicate addressMatch = cb.like(cb.lower(address.get("address")), kw);
                Optional<SportType> keywordSportType = resolveSportTypeKeyword(command.keyword());
                if (keywordSportType.isPresent()) {
                    var court = root.join("venueCourts", JoinType.LEFT);
                    Predicate activeCourt = cb.isTrue(court.get("active"));
                    Predicate sportMatch = cb.equal(court.get("sportType"), keywordSportType.get());
                    predicates.add(cb.or(nameMatch, descMatch, addressMatch, cb.and(activeCourt, sportMatch)));
                    query.distinct(true);
                } else {
                    predicates.add(cb.or(nameMatch, descMatch, addressMatch));
                }
            }

            if (command.city() != null && !command.city().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.join("address", JoinType.INNER).get("city")), command.city().trim().toLowerCase()));
            }

            if (command.district() != null && !command.district().trim().isEmpty()) {
                String area = "%" + command.district().trim().toLowerCase() + "%";
                var address = root.join("address", JoinType.INNER);
                Predicate districtMatch = cb.like(cb.lower(address.get("district")), area);
                Predicate wardMatch = cb.like(cb.lower(address.get("ward")), area);
                predicates.add(cb.or(districtMatch, wardMatch));
            }

            if (hasViewportBounds(command)) {
                var address = root.join("address", JoinType.INNER);
                predicates.add(cb.between(
                        address.get("latitude"),
                        command.southLatitude(),
                        command.northLatitude()
                ));
                predicates.add(cb.between(
                        address.get("longitude"),
                        command.westLongitude(),
                        command.eastLongitude()
                ));
            }

            if (command.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("minPrice"), command.minPrice()));
            }

            if (command.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("minPrice"), command.maxPrice()));
            }

            if (command.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), command.minRating()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int page = Math.max(0, command.page());
        int size = command.size() > 0 ? Math.min(command.size(), 100) : 10;
        Pageable pageable = PageRequest.of(page, size);

        if (hasRadiusFilter(command)) {
            List<VenueEntity> nearbyVenues = jpaRepository.findAll(spec).stream()
                    .filter(entity -> hasCoordinates(entity)
                            && calculateDistance(
                                    command.latitude(), command.longitude(),
                                    entity.getAddress().getLatitude(), entity.getAddress().getLongitude()
                            ) <= command.radiusKm())
                    .sorted(Comparator.comparingDouble(entity -> calculateDistance(
                            command.latitude(), command.longitude(),
                            entity.getAddress().getLatitude(), entity.getAddress().getLongitude()
                    )))
                    .toList();

            int fromIndex = Math.min(page * size, nearbyVenues.size());
            int toIndex = Math.min(fromIndex + size, nearbyVenues.size());
            List<Venue> items = nearbyVenues.subList(fromIndex, toIndex).stream()
                    .map(venueEntityMapper::toDomain)
                    .toList();
            int totalPages = nearbyVenues.isEmpty() ? 0 : (int) Math.ceil((double) nearbyVenues.size() / size);

            return new PageResult<>(items, nearbyVenues.size(), page, size, totalPages);
        }

        Page<VenueEntity> pagedResult = jpaRepository.findAll(spec, pageable);

        List<Venue> items = pagedResult.getContent().stream()
                .map(venueEntityMapper::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(
                items,
                pagedResult.getTotalElements(),
                pagedResult.getNumber(),
                pagedResult.getSize(),
                pagedResult.getTotalPages()
        );
    }

    private boolean hasRadiusFilter(SearchVenueCommand command) {
        return command.latitude() != null
                && command.longitude() != null
                && command.radiusKm() != null
                && command.radiusKm() > 0;
    }

    private boolean hasViewportBounds(SearchVenueCommand command) {
        return command.northLatitude() != null
                && command.southLatitude() != null
                && command.eastLongitude() != null
                && command.westLongitude() != null
                && command.northLatitude() >= command.southLatitude()
                && command.eastLongitude() >= command.westLongitude();
    }

    static Optional<SportType> resolveSportTypeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }

        String normalizedKeyword = normalizeSearchText(keyword);
        return List.of(SportType.values()).stream()
                .filter(sportType -> normalizedKeyword.equals(normalizeSearchText(sportType.getValue()))
                        || normalizedKeyword.equals(normalizeSearchText(sportType.name())))
                .findFirst();
    }

    private static String normalizeSearchText(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private boolean hasCoordinates(VenueEntity venue) {
        return venue.getAddress() != null
                && venue.getAddress().getLatitude() != null
                && venue.getAddress().getLongitude() != null;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusKm = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    public PageResult<Venue> findAll(AdminVenueFilter filter) {
        Specification<VenueEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(switch (filter.status()) {
                    case ACTIVE -> cb.isTrue(root.get("active"));
                    case INACTIVE -> cb.and(cb.isFalse(root.get("active")), cb.isNull(root.get("suspendedAt")));
                    case SUSPENDED -> cb.isNotNull(root.get("suspendedAt"));
                });
            }
            if (filter.query() != null && !filter.query().isBlank()) {
                String like = "%" + filter.query().trim().toLowerCase(Locale.ROOT) + "%";
                var address = root.join("address", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(root.get("phone"), like),
                        cb.like(cb.lower(address.get("district")), like),
                        cb.like(cb.lower(address.get("city")), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<VenueEntity> result = jpaRepository.findAll(spec, PageRequest.of(
                filter.page(), filter.size(), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("venueId"))
        ));
        return new PageResult<>(
                result.getContent().stream().map(venueEntityMapper::toDomain).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages()
        );
    }
}
