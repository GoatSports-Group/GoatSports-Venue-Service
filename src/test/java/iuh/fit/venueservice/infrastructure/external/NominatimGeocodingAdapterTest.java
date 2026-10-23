package iuh.fit.venueservice.infrastructure.external;

import com.sun.net.httpserver.HttpServer;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.infrastructure.external.adapter.NominatimGeocodingAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NominatimGeocodingAdapterTest {
    private static final String REVERSE = """
            {"lat":"10.7769","lon":"106.7009","name":"","display_name":"Bến Nghé, Quận 1, Hồ Chí Minh",
             "address":{"road":"Lê Lợi","suburb":"Phường Bến Nghé","city_district":"Quận 1","city":"Hồ Chí Minh"}}
            """;
    private static final String SEARCH = """
            [{"lat":"11.94","lon":"108.44","name":"Tỉnh Lâm Đồng","display_name":"Tỉnh Lâm Đồng, Việt Nam",
              "boundingbox":["10.98","12.84","107.28","108.98"],"address":{"state":"Tỉnh Lâm Đồng"}}]
            """;

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int failuresLeft;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/reverse", exchange -> {
            calls.incrementAndGet();
            byte[] body = failuresLeft-- > 0 ? new byte[0] : REVERSE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(body.length == 0 ? 503 : 200, body.length == 0 ? -1 : body.length);
            if (body.length > 0) exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/search", exchange -> {
            byte[] body = SEARCH.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private NominatimGeocodingAdapter adapter() {
        return new NominatimGeocodingAdapter("http://127.0.0.1:" + server.getAddress().getPort(), "test",
                Duration.ofHours(24), 100);
    }

    @Test
    void aFailedLookupIsNotRememberedSoTheNextRequestRecovers() {
        failuresLeft = 1;
        NominatimGeocodingAdapter adapter = adapter();

        assertThat(adapter.reverse(10.7769, 106.7009)).isEmpty();
        assertThat(adapter.reverse(10.7769, 106.7009))
                .hasValueSatisfying(result -> assertThat(result.address())
                        .isEqualTo("Lê Lợi, Phường Bến Nghé, Quận 1, Hồ Chí Minh"));
        assertThat(calls).hasValue(2);
    }

    @Test
    void aProvinceSearchCarriesItsBoundingBoxAsSouthWestNorthEast() {
        assertThat(adapter().search("Lâm Đồng")).singleElement()
                .satisfies(result -> assertThat(result.bounds()).isEqualTo(new GeoBounds(10.98, 107.28, 12.84, 108.98)));
    }

    @Test
    void aSuccessfulLookupIsCached() {
        NominatimGeocodingAdapter adapter = adapter();
        assertThat(adapter.reverse(10.7769, 106.7009)).isPresent();
        assertThat(adapter.reverse(10.7769, 106.7009)).isPresent();
        assertThat(calls).hasValue(1);
    }
}
