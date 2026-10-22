package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.SportType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ô tìm kiếm: kết quả theo nghĩa (ai-service) GỘP với LIKE chứ không thay thế. Chỉ mục ai-service có thể thiếu sân,
 * thay thế thì gõ đúng tên sân vẫn ra 0 kết quả.
 */
@SpringBootTest
@ActiveProfiles("test")
class VenueKeywordSearchTest {

    @Autowired private VenueRepository venueRepository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void semanticHitsAreMergedWithNameMatchesAndSavedIdsStillRestrict() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        UUID named = insertVenue("Alpha " + tag, "BADMINTON");
        UUID football = insertVenue("Bravo " + tag, "FOOTBALL");
        UUID semanticOnly = insertVenue("Charlie " + tag, "TENNIS");

        // Không có trong chỉ mục nhưng đúng tên vẫn ra; sân chỉ khớp theo nghĩa cũng ra.
        assertThat(ids(keyword("Alpha " + tag).withSemanticVenueIds(List.of(semanticOnly))))
                .contains(named, semanticOnly).doesNotContain(football);
        // AI trả lời "không khớp" không được xóa kết quả LIKE.
        assertThat(ids(keyword("Alpha " + tag).withSemanticVenueIds(List.of()))).contains(named);
        // "Sân bóng đá" là môn bóng đá dù không có trong tên.
        assertThat(ids(keyword("Sân bóng đá"))).contains(football).doesNotContain(named, semanticOnly);
        // Trang "Sân đã lưu": danh sách id là giới hạn cứng, kể cả khi có kết quả theo nghĩa.
        assertThat(ids(keyword(tag).withSemanticVenueIds(List.of(semanticOnly)).withVenueIds(List.of(named))))
                .containsExactly(named);
    }

    @Test
    void cheapestFirstPutsVenuesWithoutAPriceLast() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        UUID pricey = insertVenue("Gia " + tag + " A", "TENNIS");
        UUID cheap = insertVenue("Gia " + tag + " B", "TENNIS");
        UUID unpriced = insertVenue("Gia " + tag + " C", "TENNIS");
        jdbc.update("update venues set min_price = ? where venue_id = ?", 300_000, pricey);
        jdbc.update("update venues set min_price = ? where venue_id = ?", 90_000, cheap);

        assertThat(ids(keyword("Gia " + tag).withSort(SearchVenueCommand.Sort.PRICE)))
                .containsExactly(cheap, pricey, unpriced);
    }

    @Test
    void areaMatchesTheProvinceAndTheWardNotOnlyTheDistrict() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        UUID venue = insertVenue("Khu " + tag, "FOOTBALL");
        jdbc.update("update addresses set city = ?, ward = ? where address_id = (select address_id from venues where venue_id = ?)",
                "Khánh Hòa " + tag, "Phường Nam Nha Trang " + tag, venue);

        assertThat(ids(area("Khánh Hòa " + tag))).containsExactly(venue);
        assertThat(ids(area("nha trang " + tag))).containsExactly(venue);
    }

    private SearchVenueCommand area(String area) {
        return new SearchVenueCommand(null, null, area, null, null, null, null,
                null, null, null, null, null, null, null, 0, 20);
    }

    @Test
    void sportKeywordNeedsWholeWords() {
        assertThat(VenueRepositoryImpl.resolveSportTypeKeyword("Sân bóng đá quận 7"))
                .contains(SportType.FOOTBALL);
        assertThat(VenueRepositoryImpl.resolveSportTypeKeyword("bóng")).isEmpty();
    }

    private SearchVenueCommand keyword(String keyword) {
        return new SearchVenueCommand(keyword, null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 20);
    }

    private List<UUID> ids(SearchVenueCommand command) {
        return venueRepository.search(command).items().stream().map(Venue::getVenueId).toList();
    }

    private UUID insertVenue(String name, String sportType) {
        UUID addressId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        jdbc.update("insert into addresses (address_id, address) values (?, 'Q1')", addressId);
        jdbc.update("insert into venues (venue_id, name, owner_id, address_id, active) values (?, ?, ?, ?, true)",
                venueId, name, UUID.randomUUID(), addressId);
        jdbc.update("insert into venue_courts (venue_court_id, name, sport_type, venue_id, active) values (?, 'A', ?, ?, true)",
                UUID.randomUUID(), sportType, venueId);
        return venueId;
    }
}
