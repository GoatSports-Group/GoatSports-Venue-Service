package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.persistence.entity.VenueEntity;

import java.util.Collections;
import java.util.stream.Collectors;

public class VenueMapper {

    public static VenueEntity toEntity(Venue domain) {
        if (domain == null) return null;

        VenueEntity entity =
                VenueEntity.builder()
                        .venueId(domain.getVenueId())
                        .name(domain.getName())
                        .description(domain.getDescription())
                        .openTime(domain.getOpenTime())
                        .closeTime(domain.getCloseTime())
                        .active(domain.getActive())
                        .minPrice(domain.getMinPrice())
                        .maxPrice(domain.getMaxPrice())
                        .averageRating(domain.getAverageRating())
                        .totalReviews(domain.getTotalReviews())
                        .phone(domain.getPhone())
                        .email(domain.getEmail())
                        .venueOwnerProfileEntity(VenueOwnerProfileMapper.toEntity(domain.getVenueOwnerProfile()))
                        .build();

        if (domain.getCourts() != null) {
            entity.setVenueCourtEntities(
                domain.getCourts().stream()
                    .map(court -> VenueCourtMapper.toEntity(court, entity))
                    .collect(Collectors.toSet())
            );
        }

        return entity;
    }

    public static Venue toDomain(VenueEntity entity) {
        if (entity == null) return null;

        return Venue.builder()
            .venueId(entity.getVenueId())
            .name(entity.getName())
            .description(entity.getDescription())
            .openTime(entity.getOpenTime())
            .closeTime(entity.getCloseTime())
            .active(entity.getActive())
            .minPrice(entity.getMinPrice())
            .maxPrice(entity.getMaxPrice())
            .averageRating(entity.getAverageRating())
            .totalReviews(entity.getTotalReviews())
            .phone(entity.getPhone())
            .email(entity.getEmail())
            .venueOwnerProfile(VenueOwnerProfileMapper.toDomain(entity.getVenueOwnerProfileEntity()))
            .courts(
                entity.getVenueCourtEntities() != null
                    ? entity.getVenueCourtEntities().stream()
                        .map(VenueCourtMapper::toDomain)
                        .collect(Collectors.toSet())
                    : Collections.emptySet()
            )
            .build();
    }
}
