package iuh.fit.venueservice.infrastructure.external.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.venueservice.application.dto.response.address.AddressSuggestionCommand;
import iuh.fit.venueservice.application.port.AddressSuggestionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class VietMapAddressSuggestionAdapter implements AddressSuggestionPort {
    private final RestClient restClient;
    private final String apiKey;
    private final Duration cacheTtl;
    private final int maxCacheEntries;
    private final Map<String, CacheEntry<List<AddressSuggestionCommand>>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<AddressSuggestionCommand>> suggestionCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<Optional<AddressSuggestionCommand>>> placeCache = new ConcurrentHashMap<>();

    @Autowired
    public VietMapAddressSuggestionAdapter(
            @Value("${vietmap.api.base-url}") String baseUrl,
            @Value("${vietmap.api.key}") String apiKey,
            @Value("${vietmap.cache.ttl}") Duration cacheTtl,
            @Value("${vietmap.cache.max-entries}") int maxCacheEntries
    ) {
        this(RestClient.builder().baseUrl(baseUrl).build(), apiKey, cacheTtl, maxCacheEntries);
    }

    VietMapAddressSuggestionAdapter(
            RestClient restClient,
            String apiKey,
            Duration cacheTtl,
            int maxCacheEntries
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.cacheTtl = cacheTtl;
        this.maxCacheEntries = Math.max(100, maxCacheEntries);
    }

    @Override
    public List<AddressSuggestionCommand> search(String query) {
        requireApiKey();
        String cacheKey = query.trim().toLowerCase(Locale.ROOT);
        List<AddressSuggestionCommand> cached = getCached(searchCache, cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            VietMapAutocompleteResult[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/autocomplete/v4")
                            .queryParam("apikey", apiKey)
                            .queryParam("text", query)
                            .queryParam("display_type", 5)
                            .build())
                    .retrieve()
                    .body(VietMapAutocompleteResult[].class);

            List<AddressSuggestionCommand> results = new ArrayList<>();
            if (response != null) {
                for (VietMapAutocompleteResult item : response) {
                    mapSuggestion(item).ifPresent(results::add);
                    if (results.size() == 6) {
                        break;
                    }
                }
            }
            List<AddressSuggestionCommand> immutableResults = List.copyOf(results);
            immutableResults.forEach(result -> putCached(suggestionCache, result.refId(), result));
            putCached(searchCache, cacheKey, immutableResults);
            return immutableResults;
        } catch (RestClientException exception) {
            logProviderFailure("autocomplete", exception);
            throw new IllegalStateException("Không thể tải gợi ý địa chỉ lúc này.");
        }
    }

    @Override
    public Optional<AddressSuggestionCommand> resolve(String refId) {
        requireApiKey();
        Optional<AddressSuggestionCommand> cached = getCached(placeCache, refId);
        if (cached != null) {
            return cached;
        }

        try {
            VietMapPlaceResult response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/place/v4")
                            .queryParam("apikey", apiKey)
                            .queryParam("refid", refId)
                            .build())
                    .retrieve()
                    .body(VietMapPlaceResult.class);
            Optional<AddressSuggestionCommand> result = mapPlace(refId, response);
            putCached(placeCache, refId, result);
            return result;
        } catch (RestClientException exception) {
            logProviderFailure("place", exception);
            throw new IllegalStateException("Không thể tải chi tiết địa chỉ lúc này.");
        }
    }

    private Optional<AddressSuggestionCommand> mapSuggestion(VietMapAutocompleteResult result) {
        if (result == null) {
            return Optional.empty();
        }
        String refId = normalize(result.refId());
        String title = normalize(result.name());
        String formattedAddress = firstNonBlank(result.display(), joinAddressParts(title, result.address()));
        if (refId.isBlank() || title.isBlank() || formattedAddress.isBlank()) {
            return Optional.empty();
        }

        List<VietMapBoundary> currentBoundaries = safeBoundaries(result.boundaries());
        List<VietMapBoundary> legacyBoundaries = result.legacyData() == null
                ? List.of()
                : safeBoundaries(result.legacyData().boundaries());
        return Optional.of(new AddressSuggestionCommand(
                refId,
                title,
                formattedAddress,
                title,
                boundaryName(currentBoundaries, 2),
                firstNonBlank(boundaryName(currentBoundaries, 1), boundaryName(legacyBoundaries, 1)),
                firstNonBlank(boundaryName(currentBoundaries, 0), boundaryName(legacyBoundaries, 0)),
                null,
                null
        ));
    }

    private Optional<AddressSuggestionCommand> mapPlace(String refId, VietMapPlaceResult place) {
        if (place == null || place.latitude() == null || place.longitude() == null) {
            return Optional.empty();
        }
        AddressSuggestionCommand suggestion = getCached(suggestionCache, refId);
        String streetAddress = joinAddressParts(place.houseNumber(), place.street());
        String formattedAddress = firstNonBlank(
                place.display(),
                suggestion == null ? null : suggestion.formattedAddress(),
                place.address(),
                streetAddress,
                place.name()
        );
        if (formattedAddress.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new AddressSuggestionCommand(
                refId,
                firstNonBlank(place.name(), suggestion == null ? null : suggestion.title(), streetAddress, formattedAddress),
                formattedAddress,
                formattedAddress,
                firstNonBlank(normalizeAdminName(place.ward(), 2), suggestion == null ? null : suggestion.ward()),
                firstNonBlank(normalizeAdminName(place.district(), 1), suggestion == null ? null : suggestion.district()),
                firstNonBlank(normalizeAdminName(place.city(), 0), suggestion == null ? null : suggestion.city()),
                place.latitude(),
                place.longitude()
        ));
    }

    private List<VietMapBoundary> safeBoundaries(List<VietMapBoundary> boundaries) {
        return boundaries == null ? List.of() : boundaries;
    }

    private String boundaryName(List<VietMapBoundary> boundaries, int type) {
        return boundaries.stream()
                .filter(boundary -> boundary.type() == type)
                .findFirst()
                .map(boundary -> normalizeAdminName(firstNonBlank(boundary.name(), boundary.fullName()), type))
                .orElse("");
    }

    private String normalizeAdminName(String value, int type) {
        String normalized = normalize(value);
        return switch (type) {
            case 0 -> normalized.replaceFirst("(?iu)^(?:Thành\\s+phố|Tỉnh)\\s+", "").trim();
            case 1 -> normalized.replaceFirst("(?iu)^(?:Quận|Huyện|Thị\\s+xã|Thành\\s+phố)\\s+", "").trim();
            case 2 -> normalized.replaceFirst("(?iu)^(?:Phường|Xã|Thị\\s+trấn)\\s+", "").trim();
            default -> normalized;
        };
    }

    private String joinAddressParts(String... values) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                parts.add(normalized);
            }
        }
        return String.join(", ", parts);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private void requireApiKey() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("VietMap API key chưa được cấu hình trên venue-service.");
        }
    }

    private void logProviderFailure(String operation, RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            log.warn("VietMap {} request failed with HTTP {}", operation, responseException.getStatusCode().value());
            return;
        }
        log.warn("VietMap {} request failed ({})", operation, exception.getClass().getSimpleName());
    }

    private <T> T getCached(Map<String, CacheEntry<T>> cache, String key) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    private <T> void putCached(Map<String, CacheEntry<T>> cache, String key, T value) {
        if (cache.size() >= maxCacheEntries) {
            Instant now = Instant.now();
            cache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
            if (cache.size() >= maxCacheEntries) {
                cache.keySet().stream().findFirst().ifPresent(cache::remove);
            }
        }
        cache.put(key, new CacheEntry<>(value, Instant.now().plus(cacheTtl)));
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VietMapBoundary(
            int type,
            String name,
            @JsonProperty("full_name") String fullName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VietMapAutocompleteResult(
            @JsonProperty("ref_id") String refId,
            String name,
            String display,
            String address,
            List<VietMapBoundary> boundaries,
            @JsonProperty("data_old") VietMapAutocompleteResult legacyData
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record VietMapPlaceResult(
            String display,
            String name,
            @JsonProperty("hs_num") String houseNumber,
            String street,
            String address,
            String city,
            String district,
            String ward,
            @JsonProperty("lat") Double latitude,
            @JsonProperty("lng") Double longitude
    ) {
    }
}
