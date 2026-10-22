package iuh.fit.venueservice.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemoSimulationSeederTest {

    @Test
    void everyCityOffersTheCommonSportsWithUniqueIds() {
        List<DemoSimulationSeeder.Venue> venues = DemoSimulationSeeder.venues();
        assertThat(venues).hasSize(36);
        assertThat(venues.stream().map(DemoSimulationSeeder.Venue::id).distinct()).hasSize(venues.size());
        // Id phải giống nhau giữa các lần chạy JVM (thứ tự lặp của Map.of thì không), nếu không mỗi lần khởi động
        // lại sinh thêm một bộ cơ sở mới.
        assertThat(venues).anyMatch(venue -> venue.name().equals("Cầu Lông Bình Minh")
                && venue.id().toString().equals("32a1ec2a-a903-3598-acdb-ec31d1f79669"));
        for (DemoPopulation.City city : DemoPopulation.CITIES) {
            List<DemoSimulationSeeder.Venue> local = venues.stream().filter(venue -> venue.city() == city).toList();
            assertThat(local).as(city.name()).anyMatch(venue -> venue.offers("FOOTBALL"));
            assertThat(local).as(city.name()).anyMatch(venue -> venue.offers("BADMINTON"));
            // Chủ sân chỉ quản lý cơ sở ở thành phố của mình.
            assertThat(local).allMatch(venue -> venue.owner().city() == city);
        }
        assertThat(venues).allSatisfy(venue -> assertThat(venue.courts())
                .allMatch(court -> court.price() >= 50_000 && court.price() % 10_000 == 0));
    }

    @Test
    void bookingDemandGrowsOverTimeAndPeaksOnWeekends() {
        DemoPopulation.Player player = DemoPopulation.player(0);
        LocalDate today = LocalDate.of(2026, 10, 16);
        LocalDate first = today.minusDays(DemoPopulation.HISTORY_DAYS);
        LocalDate monday = LocalDate.of(2026, 10, 12);
        double early = DemoSimulationSeeder.dailyProbability(player, first, first, today);
        double late = DemoSimulationSeeder.dailyProbability(player, monday, first, today);
        double weekend = DemoSimulationSeeder.dailyProbability(player, monday.plusDays(5), first, today);
        assertThat(early).isLessThan(late);
        assertThat(weekend).isGreaterThan(late).isLessThanOrEqualTo(0.9);
    }
}
