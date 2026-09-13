package iuh.fit.venueservice.infrastructure.external.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VietMapAddressSuggestionAdapterTest {
    private MockRestServiceServer server;
    private VietMapAddressSuggestionAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new VietMapAddressSuggestionAdapter(
                builder.baseUrl("https://maps.vietmap.vn/api").build(),
                "server-only-key", Duration.ofHours(1), 100
        );
    }

    @Test
    void mapsAutocompleteAndKeepsLegacyDistrictWhenResolvingPlace() {
        server.expect(requestToUriTemplate(
                        "https://maps.vietmap.vn/api/autocomplete/v4?apikey={key}&text={text}&display_type=5",
                        "server-only-key", "12 Nguyễn Văn Bảo"))
                .andExpect(queryParam("apikey", "server-only-key"))
                .andRespond(withSuccess("""
                        [{
                          "ref_id":"geocode:abc",
                          "name":"12 Nguyễn Văn Bảo",
                          "display":"12 Nguyễn Văn Bảo, Phường Hạnh Thông, Thành Phố Hồ Chí Minh",
                          "boundaries":[
                            {"type":2,"name":"Hạnh Thông","full_name":"Phường Hạnh Thông"},
                            {"type":0,"name":"Hồ Chí Minh","full_name":"Thành Phố Hồ Chí Minh"}
                          ],
                          "data_old":{"boundaries":[{"type":1,"name":"Gò Vấp","full_name":"Quận Gò Vấp"}]}
                        }]
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestToUriTemplate(
                        "https://maps.vietmap.vn/api/place/v4?apikey={key}&refid={refId}",
                        "server-only-key", "geocode:abc"))
                .andRespond(withSuccess("""
                        {
                          "display":"12 Nguyễn Văn Bảo, Phường Hạnh Thông, Thành phố Hồ Chí Minh",
                          "name":"12 Nguyễn Văn Bảo",
                          "ward":"Phường Hạnh Thông",
                          "district":"",
                          "city":"Thành Phố Hồ Chí Minh",
                          "lat":10.8223582,
                          "lng":106.6875313
                        }
                        """, MediaType.APPLICATION_JSON));

        var suggestions = adapter.search("12 Nguyễn Văn Bảo");
        assertThat(suggestions).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.ward()).isEqualTo("Hạnh Thông");
            assertThat(suggestion.district()).isEqualTo("Gò Vấp");
            assertThat(suggestion.city()).isEqualTo("Hồ Chí Minh");
        });

        var resolved = adapter.resolve("geocode:abc").orElseThrow();
        assertThat(resolved.district()).isEqualTo("Gò Vấp");
        assertThat(resolved.latitude()).isEqualTo(10.8223582);
        assertThat(resolved.longitude()).isEqualTo(106.6875313);
        server.verify();
    }

    @Test
    void refusesProviderCallsWhenServerKeyIsMissing() {
        var adapterWithoutKey = new VietMapAddressSuggestionAdapter(
                RestClient.builder().baseUrl("https://maps.vietmap.vn/api").build(),
                "", Duration.ofHours(1), 100
        );

        assertThatThrownBy(() -> adapterWithoutKey.search("Nguyễn Văn Bảo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("venue-service");
    }
}
