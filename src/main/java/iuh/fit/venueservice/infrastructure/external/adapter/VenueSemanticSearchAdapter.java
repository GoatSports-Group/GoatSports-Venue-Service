package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.port.VenueSemanticSearchPort;
import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.infrastructure.external.client.AiSemanticSearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VenueSemanticSearchAdapter implements VenueSemanticSearchPort {
    private static final String DOCUMENT_TYPE = "VENUE";

    private final AiSemanticSearchClient client;

    @Value("${venue.semantic-search.enabled}")
    private boolean enabled;

    @Override
    public Optional<List<UUID>> findVenueIds(String query, int maxResults) {
        if (!enabled || query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            List<AiSemanticSearchClient.SemanticSearchResult> results =
                    client.search(query.trim(), DOCUMENT_TYPE, maxResults);
            if (results == null) {
                return Optional.empty();
            }
            return Optional.of(results.stream()
                    .filter(result -> result.source_id() != null)
                    .sorted(Comparator.comparing(
                            AiSemanticSearchClient.SemanticSearchResult::score,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .map(AiSemanticSearchClient.SemanticSearchResult::source_id)
                    .distinct()
                    .toList());
        } catch (Exception exception) {
            // Nuot loi co chu dich: semantic search la enhancement, AI hong thi
            // search filter truyen thong van phai tra ket qua.
            log.warn("Semantic search không dùng được, quay về search filter: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void index(Venue venue, List<String> sportTypes) {
        if (!enabled || venue == null || venue.getVenueId() == null) {
            return;
        }
        try {
            client.index(new AiSemanticSearchClient.SemanticDocumentRequest(
                    DOCUMENT_TYPE,
                    venue.getVenueId(),
                    buildContent(venue, sportTypes),
                    buildMetadata(venue, sportTypes),
                    OffsetDateTime.now()
            ));
        } catch (Exception exception) {
            log.warn("Không lập chỉ mục được venue {}: {}", venue.getVenueId(), exception.getMessage());
        }
    }

    private String buildContent(Venue venue, List<String> sportTypes) {
        List<String> parts = new ArrayList<>();
        parts.add(venue.getName());
        parts.add(venue.getDescription());
        if (sportTypes != null) parts.addAll(sportTypes);
        if (venue.getAmenities() != null) parts.addAll(venue.getAmenities());
        Address address = venue.getAddress();
        if (address != null) {
            parts.add(address.getAddress());
            parts.add(address.getWard());
            parts.add(address.getDistrict());
            parts.add(address.getCity());
        }
        String content = parts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .distinct()
                .reduce((a, b) -> a + " " + b)
                .orElse("");
        return content.isBlank() ? venue.getVenueId().toString() : content;
    }

    private Map<String, Object> buildMetadata(Venue venue, List<String> sportTypes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("venueId", venue.getVenueId().toString());
        if (venue.getAddress() != null) {
            metadata.put("city", venue.getAddress().getCity());
            metadata.put("district", venue.getAddress().getDistrict());
        }
        if (sportTypes != null && !sportTypes.isEmpty()) {
            metadata.put("sportTypes", sportTypes);
        }
        metadata.put("active", Boolean.TRUE.equals(venue.getActive()));
        return metadata;
    }
}
