package iuh.fit.venueservice.infrastructure.external.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OverpassExternalVenueAdapterTest {

    private static JsonNode json(String value) throws Exception {
        return new ObjectMapper().readTree(value);
    }

    @Test
    void mapsNamedCourtsAndSkipsUnnamedOrUnsupportedOnes() throws Exception {
        JsonNode root = json("""
                {"elements": [
                  {"type": "way", "id": 11, "center": {"lat": 10.80028, "lon": 106.67007},
                   "tags": {"leisure": "pitch", "sport": "badminton;table_tennis", "name": "CLB Cầu Lông Hoàng Văn Thụ",
                            "addr:housenumber": "12", "addr:street": "Hoàng Văn Thụ", "addr:city": "Hồ Chí Minh",
                            "contact:phone": "+84 28 1234 5678", "opening_hours": "Mo-Su 05:00-22:00"}},
                  {"type": "node", "id": 22, "lat": 10.76848, "lon": 106.66556,
                   "tags": {"leisure": "pitch", "sport": "soccer;basketball", "name": "Sân Empire City"}},
                  {"type": "way", "id": 33, "center": {"lat": 10.1, "lon": 106.1},
                   "tags": {"leisure": "pitch", "sport": "tennis"}},
                  {"type": "way", "id": 44, "center": {"lat": 10.2, "lon": 106.2},
                   "tags": {"leisure": "pitch", "sport": "table_tennis", "name": "Bàn bóng bàn"}}
                ]}""");

        List<ExternalVenue> venues = OverpassExternalVenueAdapter.parse(root);

        assertThat(venues).hasSize(2);
        ExternalVenue badminton = venues.getFirst();
        assertThat(badminton.getSourceId()).isEqualTo("way/11");
        assertThat(badminton.getSource()).isEqualTo(ExternalVenue.SOURCE_OSM);
        assertThat(badminton.getSportTypes()).containsExactly(SportType.BADMINTON);
        assertThat(badminton.getLatitude()).isEqualTo(10.80028);
        assertThat(badminton.getAddress()).isEqualTo("12 Hoàng Văn Thụ, Hồ Chí Minh");
        assertThat(badminton.getPhone()).isEqualTo("+84 28 1234 5678");
        assertThat(badminton.getOpeningHours()).isEqualTo("Mo-Su 05:00-22:00");
        ExternalVenue node = venues.get(1);
        assertThat(node.getSourceId()).isEqualTo("node/22");
        assertThat(node.getSportTypes()).containsExactlyInAnyOrder(SportType.FOOTBALL, SportType.BASKETBALL);
        assertThat(node.getAddress()).isNull();
    }

    @Test
    void queryAsksForNamedPublicCourtsInsideTheBox() {
        String query = OverpassExternalVenueAdapter.query(new GeoBounds(10.6, 106.5, 10.9, 106.9));

        assertThat(query).contains("(10.6,106.5,10.9,106.9)").contains("[\"name\"]")
                .contains("[\"access\"!~\"^(private|no)$\"]").contains("out center tags;");
    }
}
