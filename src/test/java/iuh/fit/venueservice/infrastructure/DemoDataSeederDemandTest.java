package iuh.fit.venueservice.infrastructure;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataSeederDemandTest {

    @Test
    void demandFollowsEveningAndWeekendPeaks() {
        LocalDate friday = LocalDate.of(2026, 10, 9);
        LocalDate saturday = friday.plusDays(1);

        assertThat(DemoDataSeeder.demandProbability(friday, 19)).isGreaterThan(DemoDataSeeder.demandProbability(friday, 15));
        assertThat(DemoDataSeeder.demandProbability(saturday, 10)).isGreaterThan(DemoDataSeeder.demandProbability(friday, 10));
        assertThat(DemoDataSeeder.demandProbability(friday, 3)).isLessThan(0.1);
    }

    @Test
    void idBasedDrawIsStableUniformAndMatchesTheProbability() {
        int hits = 0;
        int samples = 20_000;
        for (int index = 0; index < samples; index++) {
            UUID id = UUID.nameUUIDFromBytes(("goatsports:demand-booking:" + index).getBytes(StandardCharsets.UTF_8));
            double unit = DemoDataSeeder.unitFromId(id);
            assertThat(unit).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
            // Cùng id luôn ra cùng số: chạy seeder lại không sinh lượt đặt khác.
            assertThat(DemoDataSeeder.unitFromId(id)).isEqualTo(unit);
            if (unit < 0.78) hits++;
        }
        assertThat(hits / (double) samples).isBetween(0.76, 0.80);
    }
}
