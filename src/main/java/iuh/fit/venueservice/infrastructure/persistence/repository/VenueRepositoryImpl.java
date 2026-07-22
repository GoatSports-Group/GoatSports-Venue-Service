package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.persistence.mapper.VenueEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VenueRepositoryImpl implements VenueRepository {
    private final JpaVenueRepository jpaRepository;
    private final VenueEntityMapper venueEntityMapper;

    @Override
    public Venue save(Venue venue) {
        var entity = venueEntityMapper.createEntity(venue);
        var saved = jpaRepository.save(entity);
        return venueEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Venue> findById(UUID id) {
        return jpaRepository.findById(id).map(venueEntityMapper::toDomain);
    }

    @Override
    public List<Venue> findByUserId(UUID userId) {
        return jpaRepository.findByVenueOwnerProfile_UserId(userId).stream()
                .map(venueEntityMapper::toDomain)
                .collect(Collectors.toList());
    }
}
