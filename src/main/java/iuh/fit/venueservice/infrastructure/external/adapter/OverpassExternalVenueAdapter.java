package iuh.fit.venueservice.infrastructure.external.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.application.port.ExternalVenueSourcePort;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Lấy sân thể thao có tên trong một khung bản đồ nhỏ (một ô, xem {@code ExternalVenueServiceImpl}) từ OpenStreetMap qua
 * Overpass API. Máy chủ Overpass công khai hay quá tải (502/504), nên thử lần lượt từng máy chủ, mỗi máy 3 lần, nghỉ
 * giữa các lần. Truy vấn xin {@code timeout:60}: Overpass xếp hàng theo thời gian xin, xin ít thì dễ được nhận hơn.
 * Dữ liệu © OpenStreetMap contributors (ODbL): client phải ghi nguồn khi hiển thị.
 */
@Slf4j
@Component
public class OverpassExternalVenueAdapter implements ExternalVenueSourcePort {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);
    private static final int ATTEMPTS_PER_SERVER = 3;
    private static final Duration RETRY_PAUSE = Duration.ofSeconds(10);

    private static final Map<String, SportType> SPORTS = Map.of(
            "soccer", SportType.FOOTBALL,
            "football", SportType.FOOTBALL,
            "futsal", SportType.FOOTBALL,
            "badminton", SportType.BADMINTON,
            "tennis", SportType.TENNIS,
            "pickleball", SportType.PICKLEBALL,
            "basketball", SportType.BASKETBALL,
            "volleyball", SportType.VOLLEYBALL,
            "beachvolleyball", SportType.VOLLEYBALL
    );

    private final List<RestClient> servers;
    private final ObjectMapper objectMapper;

    public OverpassExternalVenueAdapter(
            @Value("${overpass.api.urls}") List<String> urls,
            @Value("${overpass.user-agent}") String userAgent,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.servers = urls.stream()
                .map(url -> RestClient.builder().requestFactory(requestFactory).baseUrl(url.trim())
                        .defaultHeader("User-Agent", userAgent).build())
                .toList();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ExternalVenue> fetch(GeoBounds bounds) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query(bounds));
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= ATTEMPTS_PER_SERVER; attempt++) {
            for (RestClient server : servers) {
                try {
                    String body = server.post().contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                            .retrieve().body(String.class);
                    return parse(objectMapper.readTree(body));
                } catch (RestClientException | IOException exception) {
                    lastFailure = new IllegalStateException(exception.getMessage(), exception);
                    log.warn("Overpass request failed (attempt {}): {}", attempt, exception.getMessage());
                    pause();
                }
            }
        }
        throw new IllegalStateException("Không tải được dữ liệu sân từ OpenStreetMap lúc này.", lastFailure);
    }

    /** Sân, trung tâm thể thao có tên và môn; bỏ nơi ghi rõ không cho người ngoài vào (khu dân cư, chung cư). */
    static String query(GeoBounds b) {
        return """
                [out:json][timeout:60];
                nwr["leisure"~"^(pitch|sports_centre)$"]["sport"]["name"]["access"!~"^(private|no)$"](%s,%s,%s,%s);
                out center tags;
                """.formatted(b.south(), b.west(), b.north(), b.east());
    }

    static List<ExternalVenue> parse(JsonNode root) {
        List<ExternalVenue> venues = new ArrayList<>();
        for (JsonNode element : root.path("elements")) {
            toVenue(element).ifPresent(venues::add);
        }
        return venues;
    }

    static Optional<ExternalVenue> toVenue(JsonNode element) {
        JsonNode tags = element.path("tags");
        String name = firstTag(tags, "name", "name:vi");
        Set<SportType> sports = sports(tags.path("sport").asText(""));
        JsonNode point = element.has("lat") ? element : element.path("center");
        if (name == null || sports.isEmpty() || !point.has("lat") || !point.has("lon")) {
            return Optional.empty();
        }
        return Optional.of(ExternalVenue.builder()
                .source(ExternalVenue.SOURCE_OSM)
                .sourceId(element.path("type").asText() + "/" + element.path("id").asText())
                .name(limit(name, 200))
                .sportTypes(sports)
                .latitude(point.path("lat").asDouble())
                .longitude(point.path("lon").asDouble())
                .address(limit(address(tags), 500))
                .phone(limit(firstTag(tags, "phone", "contact:phone", "contact:mobile"), 60))
                .website(limit(firstTag(tags, "website", "contact:website", "contact:facebook", "url"), 500))
                .openingHours(limit(firstTag(tags, "opening_hours"), 200))
                .build());
    }

    private static Set<SportType> sports(String value) {
        Set<SportType> sports = EnumSet.noneOf(SportType.class);
        for (String token : value.split(";")) {
            SportType sport = SPORTS.get(token.trim().toLowerCase(Locale.ROOT));
            if (sport != null) sports.add(sport);
        }
        return sports;
    }

    private static String address(JsonNode tags) {
        String street = firstTag(tags, "addr:street");
        String house = firstTag(tags, "addr:housenumber");
        StringJoiner address = new StringJoiner(", ");
        if (street != null) address.add(house == null ? street : house + " " + street);
        for (String key : List.of("addr:subdistrict", "addr:district", "addr:city")) {
            String part = firstTag(tags, key);
            if (part != null) address.add(part);
        }
        return address.length() == 0 ? null : address.toString();
    }

    private static String firstTag(JsonNode tags, String... keys) {
        for (String key : keys) {
            String value = tags.path(key).asText("").trim();
            if (!value.isEmpty()) return value;
        }
        return null;
    }

    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static void pause() {
        try {
            Thread.sleep(RETRY_PAUSE);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
