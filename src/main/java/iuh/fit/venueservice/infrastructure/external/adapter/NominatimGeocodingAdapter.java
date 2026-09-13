package iuh.fit.venueservice.infrastructure.external.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.venueservice.application.dto.response.address.GeocodingResultCommand;
import iuh.fit.venueservice.application.port.GeocodingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class NominatimGeocodingAdapter implements GeocodingPort {
    private static final Duration MIN_REQUEST_INTERVAL = Duration.ofSeconds(1);

    private final RestClient restClient;
    private final Duration cacheTtl;
    private final int maxCacheEntries;
    private final Map<String, CacheEntry<List<GeocodingResultCommand>>> searchCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<Optional<GeocodingResultCommand>>> reverseCache = new ConcurrentHashMap<>();
    private final Object providerLock = new Object();
    private long nextProviderRequestAtNanos;

    public NominatimGeocodingAdapter(
            @Value("${geocoding.nominatim.base-url}") String baseUrl,
            @Value("${geocoding.nominatim.user-agent}") String userAgent,
            @Value("${geocoding.cache.ttl}") Duration cacheTtl,
            @Value("${geocoding.cache.max-entries}") int maxCacheEntries
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Language", "vi-VN,vi;q=0.9")
                .build();
        this.cacheTtl = cacheTtl;
        this.maxCacheEntries = Math.max(100, maxCacheEntries);
    }

    @Override
    public List<GeocodingResultCommand> search(String query) {
        String cacheKey = query.trim().toLowerCase(Locale.ROOT);
        List<GeocodingResultCommand> cached = getCached(searchCache, cacheKey);
        if (cached != null) {
            return cached;
        }

        List<GeocodingResultCommand> results = callProvider(() -> {
            NominatimResult[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("format", "jsonv2")
                            .queryParam("q", query)
                            .queryParam("limit", 6)
                            .queryParam("countrycodes", "vn")
                            .queryParam("addressdetails", 1)
                            .queryParam("accept-language", "vi")
                            .build())
                    .retrieve()
                    .body(NominatimResult[].class);
            if (response == null) {
                return List.of();
            }
            List<GeocodingResultCommand> mapped = new ArrayList<>();
            for (NominatimResult item : response) {
                map(item).ifPresent(mapped::add);
            }
            return List.copyOf(mapped);
        }, List.of());
        putCached(searchCache, cacheKey, results);
        return results;
    }

    @Override
    public Optional<GeocodingResultCommand> reverse(double latitude, double longitude) {
        String cacheKey = String.format(Locale.ROOT, "%.5f,%.5f", latitude, longitude);
        Optional<GeocodingResultCommand> cached = getCached(reverseCache, cacheKey);
        if (cached != null) {
            return cached;
        }

        Optional<GeocodingResultCommand> result = callProvider(() -> {
            NominatimResult response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("zoom", 18)
                            .queryParam("addressdetails", 1)
                            .queryParam("accept-language", "vi")
                            .build())
                    .retrieve()
                    .body(NominatimResult.class);
            return map(response);
        }, Optional.empty());
        putCached(reverseCache, cacheKey, result);
        return result;
    }

    private <T> T callProvider(Supplier<T> request, T fallback) {
        synchronized (providerLock) {
            waitForProviderWindow();
            nextProviderRequestAtNanos = System.nanoTime() + MIN_REQUEST_INTERVAL.toNanos();
            try {
                return request.get();
            } catch (RestClientException exception) {
                log.warn("Nominatim geocoding request failed: {}", exception.getMessage());
                return fallback;
            }
        }
    }

    private void waitForProviderWindow() {
        long waitNanos = nextProviderRequestAtNanos - System.nanoTime();
        if (waitNanos <= 0) {
            return;
        }
        try {
            TimeUnit.NANOSECONDS.sleep(waitNanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Geocoding request was interrupted", exception);
        }
    }

    private Optional<GeocodingResultCommand> map(NominatimResult result) {
        if (result == null || result.latitude() == null || result.longitude() == null) {
            return Optional.empty();
        }
        try {
            double latitude = Double.parseDouble(result.latitude());
            double longitude = Double.parseDouble(result.longitude());
            String address = formatAddress(result);
            if (address.isBlank()) {
                return Optional.empty();
            }
            String title = firstNonBlank(
                    result.name(),
                    result.address() == null ? null : result.address().road(),
                    address.split(",", 2)[0]
            );
            return Optional.of(new GeocodingResultCommand(latitude, longitude, title, address));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String formatAddress(NominatimResult result) {
        NominatimAddress address = result.address();
        if (address == null) {
            return normalize(result.displayName());
        }

        String street = firstNonBlank(address.road(), address.pedestrian(), address.residential());
        String streetAddress = joinAddressParts(address.houseNumber(), street);
        String ward = firstNonBlank(address.quarter(), address.suburb(), address.neighbourhood(), address.village());
        String district = firstNonBlank(address.cityDistrict(), address.district(), address.county());
        String city = firstNonBlank(address.city(), address.town(), address.municipality());
        String province = firstNonBlank(address.province(), address.state());
        String detailed = joinAddressParts(address.amenity(), streetAddress, ward, district, city, province);
        return detailed.isBlank() ? normalize(result.displayName()) : detailed;
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
        return value == null ? "" : value.trim()
                .replaceAll("^,+|,+$", "")
                .replaceAll("\\s+", " ");
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
    private record NominatimResult(
            String lat,
            String lon,
            String name,
            @JsonProperty("display_name") String displayName,
            NominatimAddress address
    ) {
        String latitude() {
            return lat;
        }

        String longitude() {
            return lon;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimAddress(
            @JsonProperty("house_number") String houseNumber,
            String amenity,
            String road,
            String pedestrian,
            String residential,
            String neighbourhood,
            String quarter,
            String suburb,
            String village,
            @JsonProperty("city_district") String cityDistrict,
            String district,
            String county,
            String city,
            String town,
            String municipality,
            String province,
            String state
    ) {
    }
}
