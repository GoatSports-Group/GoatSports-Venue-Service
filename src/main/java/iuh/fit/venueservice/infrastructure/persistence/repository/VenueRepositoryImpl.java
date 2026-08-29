package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
    public PageResult<Venue> search(SearchVenueCommand command) {
        Specification<VenueEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (command.keyword() != null && !command.keyword().trim().isEmpty()) {
                String kw = "%" + command.keyword().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), kw);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), kw);
                var address = root.join("address", JoinType.INNER);
                Predicate addressMatch = cb.like(cb.lower(address.get("address")), kw);
                predicates.add(cb.or(nameMatch, descMatch, addressMatch));
            }

            if (command.city() != null && !command.city().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.join("address", JoinType.INNER).get("city")), command.city().trim().toLowerCase()));
            }

            if (command.district() != null && !command.district().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.join("address", JoinType.INNER).get("district")), command.district().trim().toLowerCase()));
            }

            if (command.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("minPrice"), command.minPrice()));
            }

            if (command.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("maxPrice"), command.maxPrice()));
            }

            if (command.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), command.minRating()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int page = Math.max(0, command.page());
        int size = command.size() > 0 ? Math.min(command.size(), 100) : 10;
        Pageable pageable = PageRequest.of(page, size);

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

    @Override
    public PageResult<Venue> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? Math.min(size, 100) : 10);
        Page<VenueEntity> result = jpaRepository.findAll(pageable);
        List<Venue> items = result.getContent().stream()
                .map(venueEntityMapper::toDomain)
                .collect(Collectors.toList());
        return new PageResult<>(
                items,
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages()
        );
    }
}
