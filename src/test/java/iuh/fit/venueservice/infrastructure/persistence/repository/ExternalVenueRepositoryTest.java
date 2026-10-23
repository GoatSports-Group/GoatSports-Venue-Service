package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ExternalVenueRepositoryTest {

    @Autowired private ExternalVenueRepository repository;

    private static ExternalVenue venue(String sourceId, String name, Set<SportType> sports, double lat, double lng,
                                       String region) {
        return ExternalVenue.builder().source(ExternalVenue.SOURCE_OSM).sourceId(sourceId).name(name)
                .sportTypes(sports).latitude(lat).longitude(lng).region(region).importedAt(LocalDateTime.now()).build();
    }

    @Test
    void wantsAndMatchesAreCountedOnceAndGoAwayWithTheirRegion() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String region = "Xóa " + tag;
        repository.upsertAll(List.of(venue("way/" + tag, "Sân " + tag, EnumSet.of(SportType.TENNIS), 22.0, 104.0, region)));
        UUID venueId = repository.findByRegion(region, 0, 5).items().getFirst().getExternalVenueId();
        UUID player = UUID.randomUUID();
        UUID session = UUID.randomUUID();

        repository.addWant(venueId, player);
        repository.addWant(venueId, player);
        repository.addWant(venueId, UUID.randomUUID());
        repository.recordMatch(session, venueId);
        repository.recordMatch(session, venueId);

        assertThat(repository.countWants(venueId)).isEqualTo(2);
        assertThat(repository.hasWant(venueId, player)).isTrue();
        assertThat(repository.findDemand()).filteredOn(demand -> demand.venue().getExternalVenueId().equals(venueId))
                .singleElement().satisfies(demand -> {
                    assertThat(demand.wantCount()).isEqualTo(2);
                    assertThat(demand.matchCount()).isEqualTo(1);
                });

        repository.removeWantsBy(player);
        assertThat(repository.hasWant(venueId, player)).isFalse();

        assertThat(repository.deleteByRegion(region)).isEqualTo(1);
        assertThat(repository.findById(venueId)).isEmpty();
        assertThat(repository.findDemand()).extracting(ExternalVenueDemand::venue)
                .noneMatch(venue -> venue.getExternalVenueId().equals(venueId));
    }

    @Test
    void reimportUpdatesTheSameRowAndFiltersByBoxSportAndName() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String region = "Test " + tag;
        String badmintonId = "way/" + tag + "1";
        repository.upsertAll(List.of(
                venue(badmintonId, "Cầu Lông " + tag, EnumSet.of(SportType.BADMINTON), 20.001, 105.001, region),
                venue("way/" + tag + "2", "Bóng Đá " + tag, EnumSet.of(SportType.FOOTBALL, SportType.BASKETBALL),
                        20.002, 105.002, region),
                venue("way/" + tag + "3", "Xa Lắm " + tag, EnumSet.of(SportType.BADMINTON), 21.5, 105.5, region)));
        GeoBounds box = new GeoBounds(20.0, 105.0, 20.01, 105.01);
        UUID firstId = repository.findInBounds(box, SportType.BADMINTON, null, 50).getFirst().getExternalVenueId();

        repository.upsertAll(List.of(
                venue(badmintonId, "Cầu Lông Mới " + tag, EnumSet.of(SportType.BADMINTON), 20.001, 105.001, region)));

        List<ExternalVenue> inBox = repository.findInBounds(box, null, null, 50);
        assertThat(inBox).extracting(ExternalVenue::getName)
                .containsExactly("Bóng Đá " + tag, "Cầu Lông Mới " + tag);
        assertThat(repository.findInBounds(box, SportType.BADMINTON, null, 50))
                .singleElement().satisfies(v -> assertThat(v.getExternalVenueId()).isEqualTo(firstId));
        assertThat(repository.findInBounds(box, SportType.BASKETBALL, null, 50))
                .extracting(ExternalVenue::getName).containsExactly("Bóng Đá " + tag);
        assertThat(repository.findInBounds(box, null, "CẦU LÔNG", 50))
                .extracting(ExternalVenue::getName).containsExactly("Cầu Lông Mới " + tag);
        assertThat(repository.summarizeByRegion()).filteredOn(s -> s.region().equals(region))
                .singleElement().extracting(ExternalVenueRegionSummary::venueCount).isEqualTo(3L);
        PageResult<ExternalVenue> secondPage = repository.findByRegion(region, 1, 2);
        assertThat(secondPage.total()).isEqualTo(3);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.items()).extracting(ExternalVenue::getName).containsExactly("Xa Lắm " + tag);
    }
}
