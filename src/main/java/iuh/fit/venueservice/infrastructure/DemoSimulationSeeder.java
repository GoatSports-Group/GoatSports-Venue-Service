package iuh.fit.venueservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Thị trường mô phỏng (dev / demo): khoảng 36 cơ sở ở 4 tỉnh/thành do các chủ sân mô phỏng quản lý, khung giờ
 * {@link #SLOT_DAYS} ngày tới, và {@link DemoPopulation#HISTORY_DAYS} ngày lượt đặt sân của người chơi mô phỏng:
 * đông vào tối ngày thường và cuối tuần, tăng dần theo thời gian; kèm check-in và đánh giá. Đủ để gợi ý sân, dự báo
 * nhu cầu, dashboard chủ sân và admin có dữ liệu thật để chạy.
 *
 * <p>Tất định: cùng ngày chạy ra cùng dữ liệu. Chạy lại chỉ bổ sung các ngày còn thiếu (lượt đặt ghi theo ngày), nên
 * khởi động hằng ngày vẫn nhanh. Bật bằng {@code app.demo-data.simulation.enabled=true}.
 */
@Slf4j
@Component
@Order(21)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data.simulation", name = "enabled", havingValue = "true")
public class DemoSimulationSeeder implements CommandLineRunner {
    static final String CREATED_BY = "demo-sim";
    private static final int SLOT_DAYS = 14;
    private static final int FUTURE_DAYS = 7;

    private final JdbcTemplate jdbc;

    record Court(UUID id, String name, String sport, int capacity, String surface, double price) { }

    record Venue(UUID id, String name, DemoPopulation.City city, String district, String ward, String street,
                 double latitude, double longitude, LocalTime open, LocalTime close, DemoPopulation.Owner owner,
                 double quality, List<String> amenities, List<Court> courts) {
        boolean offers(String sport) {
            return courts.stream().anyMatch(court -> court.sport().equals(sport));
        }
    }

    // ---- Cơ sở mô phỏng ----

    /** Số cơ sở mỗi nơi, cùng thứ tự với {@link DemoPopulation#CITIES} (thứ tự cố định thì id mới tất định). */
    private static final int[] VENUES_PER_CITY = {14, 9, 6, 7};
    private static final String[] NAMES = {"Hoàng Long", "Phượng Hoàng", "Thống Nhất", "Sao Mai", "Bình Minh",
            "Hòa Bình", "Kỳ Hòa", "Hải Âu", "Thành Công", "Chiến Thắng", "Nam Việt", "Đại Dương", "Bạch Đằng", "Lam Sơn",
            "Hùng Vương", "Đông Á", "Hưng Thịnh", "Ánh Dương", "Trường Xuân", "Phú Quý", "An Khang", "Tân Tiến",
            "Hồng Hà", "Sông Hàn", "Biển Xanh", "Vạn Phúc", "Minh Khai", "Lạc Long", "Hòa Hưng", "Phúc Lộc"};
    private static final String[] STREETS = {"Nguyễn Văn Linh", "Lê Lợi", "Trần Hưng Đạo", "Hai Bà Trưng",
            "Lý Thường Kiệt", "Phạm Văn Đồng", "Võ Văn Kiệt", "Nguyễn Trãi", "Điện Biên Phủ", "Lê Duẩn", "Hoàng Văn Thụ",
            "Trường Chinh", "Cách Mạng Tháng 8", "Phan Đăng Lưu", "Nguyễn Thị Minh Khai"};
    private static final String[] TYPES = {"FOOTBALL", "BADMINTON", "PICKLEBALL", "TENNIS", "ARENA", "COMPLEX"};
    private static final int[] TYPE_WEIGHTS = {30, 28, 15, 9, 8, 10};
    private static final Map<String, Double> BASE_PRICE = Map.of("FOOTBALL", 380_000.0, "BADMINTON", 120_000.0,
            "PICKLEBALL", 160_000.0, "TENNIS", 200_000.0, "BASKETBALL", 300_000.0, "VOLLEYBALL", 250_000.0);
    private static final Map<String, Double> CITY_PRICE = Map.of("Hồ Chí Minh", 1.0, "Hà Nội", 1.0,
            "Đà Nẵng", 0.85, "Khánh Hòa", 0.8);

    static List<Venue> venues() {
        List<Venue> venues = new ArrayList<>();
        Random random = new Random(0x5E4_2026L);
        List<DemoPopulation.Owner> owners = DemoPopulation.owners();
        int nameCursor = 0;
        for (int cityIndex = 0; cityIndex < VENUES_PER_CITY.length; cityIndex++) {
            DemoPopulation.City city = DemoPopulation.CITIES.get(cityIndex);
            List<DemoPopulation.Owner> cityOwners = owners.stream().filter(owner -> owner.city() == city).toList();
            for (int index = 0; index < VENUES_PER_CITY[cityIndex]; index++) {
                String type = TYPES[DemoPopulation.weighted(random, TYPE_WEIGHTS)];
                String brand = NAMES[nameCursor++ % NAMES.length];
                String name = switch (type) {
                    case "FOOTBALL" -> "Sân Bóng Đá Mini " + brand;
                    case "BADMINTON" -> "Cầu Lông " + brand;
                    case "PICKLEBALL" -> "Pickleball " + brand;
                    case "TENNIS" -> "Tennis " + brand;
                    case "ARENA" -> "Nhà Thi Đấu " + brand;
                    default -> "Trung Tâm Thể Thao " + brand;
                };
                String district = city.districts().get(random.nextInt(city.districts().size()));
                String ward = "Khánh Hòa".equals(city.name()) && "Nha Trang".equals(district)
                        ? "Phường " + new String[]{"Nam Nha Trang", "Bắc Nha Trang", "Tây Nha Trang"}[random.nextInt(3)]
                        : "Phường " + (1 + random.nextInt(15));
                String street = (10 + random.nextInt(480)) + " " + STREETS[random.nextInt(STREETS.length)];
                UUID venueId = id("sim-venue", city.name() + "|" + name);
                List<Court> courts = courts(type, venueId, city, random);
                boolean indoor = type.equals("BADMINTON") || type.equals("ARENA");
                List<String> amenities = new ArrayList<>(List.of("Bãi giữ xe", "Nước uống"));
                for (String amenity : new String[]{"Wifi", "Phòng thay đồ", "Căng tin", "Cho thuê dụng cụ", "Đèn chiếu sáng"}) {
                    if (random.nextInt(100) < 55) amenities.add(amenity);
                }
                if (indoor) amenities.add("Máy lạnh");
                venues.add(new Venue(venueId, name, city, district, ward, street,
                        round(city.latitude() + (random.nextDouble() - 0.5) * 0.12),
                        round(city.longitude() + (random.nextDouble() - 0.5) * 0.12),
                        LocalTime.of(random.nextBoolean() ? 5 : 6, 0), LocalTime.of(random.nextBoolean() ? 22 : 23, 0),
                        cityOwners.get(index % cityOwners.size()), 3.6 + random.nextDouble() * 1.3,
                        List.copyOf(amenities), courts));
            }
        }
        return venues;
    }

    private static List<Court> courts(String type, UUID venueId, DemoPopulation.City city, Random random) {
        List<String[]> plan = switch (type) {
            case "FOOTBALL" -> List.of(new String[]{"FOOTBALL", "Sân 5 người", "Cỏ nhân tạo", "10"},
                    new String[]{"FOOTBALL", "Sân 5 người", "Cỏ nhân tạo", "10"},
                    new String[]{"FOOTBALL", "Sân 7 người", "Cỏ nhân tạo", "14"});
            case "BADMINTON" -> repeat(new String[]{"BADMINTON", "Sân", "Sàn gỗ", "4"}, 4 + random.nextInt(3));
            case "PICKLEBALL" -> repeat(new String[]{"PICKLEBALL", "Sân", "Mặt nhựa acrylic", "4"}, 3 + random.nextInt(2));
            case "TENNIS" -> repeat(new String[]{"TENNIS", "Sân", "Mặt cứng", "4"}, 2 + random.nextInt(2));
            case "ARENA" -> List.of(new String[]{"BASKETBALL", "Sân bóng rổ", "Sàn gỗ", "10"},
                    new String[]{"VOLLEYBALL", "Sân bóng chuyền", "Sàn PVC", "12"},
                    new String[]{"VOLLEYBALL", "Sân bóng chuyền", "Sàn PVC", "12"});
            default -> List.of(new String[]{"FOOTBALL", "Sân 5 người", "Cỏ nhân tạo", "10"},
                    new String[]{"BADMINTON", "Sân cầu lông", "Thảm PVC", "4"},
                    new String[]{"BADMINTON", "Sân cầu lông", "Thảm PVC", "4"},
                    new String[]{"PICKLEBALL", "Sân pickleball", "Mặt nhựa acrylic", "4"});
        };
        List<Court> courts = new ArrayList<>();
        double priceFactor = CITY_PRICE.get(city.name()) * (0.85 + random.nextDouble() * 0.35);
        Map<String, Integer> numbering = new HashMap<>();
        for (String[] court : plan) {
            int number = numbering.merge(court[1], 1, Integer::sum);
            String name = court[1] + " " + number;
            double price = Math.round(BASE_PRICE.get(court[0]) * priceFactor * ("Sân 7 người".equals(court[1]) ? 1.4 : 1)
                    / 10_000d) * 10_000d;
            courts.add(new Court(id("sim-court", venueId + "|" + name), name, court[0], Integer.parseInt(court[3]),
                    court[2], price));
        }
        return courts;
    }

    private static List<String[]> repeat(String[] court, int count) {
        List<String[]> courts = new ArrayList<>();
        for (int index = 0; index < count; index++) courts.add(court);
        return courts;
    }

    // ---- Chạy ----

    @Override
    @Transactional
    public void run(String... args) {
        List<Venue> venues = venues();
        LocalDate today = LocalDate.now();
        int newVenues = seedVenues(venues, today);
        int slots = seedSlots(venues, today);
        BookingStats bookings = seedBookings(venues, today);
        if (bookings.inserted() > 0) {
            refreshRatings();
            // Lượt đặt ghi thẳng vào DB không phát sự kiện hành vi: cho lượt nạp bù chạy lại để ai-service học được.
            jdbc.update("delete from activity_backfills where name = 'bookings-v1'");
        }
        log.info("Dữ liệu mô phỏng: {} cơ sở mới, {} khung giờ, {} lượt đặt ({} đánh giá) từ {} tới {}",
                newVenues, slots, bookings.inserted(), bookings.reviews(), bookings.from(), bookings.to());
    }

    private int seedVenues(List<Venue> venues, LocalDate today) {
        int inserted = 0;
        Timestamp since = Timestamp.valueOf(today.minusDays(DemoPopulation.HISTORY_DAYS + 30).atStartOfDay());
        for (Venue venue : venues) {
            UUID addressId = id("sim-address", venue.id().toString());
            jdbc.update("""
                            insert into addresses (address_id, address, ward, district, city, latitude, longitude,
                                                   created_at, created_by, updated_at, updated_by)
                            values (?, ?, ?, ?, ?, ?, ?, now(), ?, now(), ?) on conflict do nothing
                            """, addressId, venue.street(), venue.ward(), venue.district(), venue.city().name(),
                    venue.latitude(), venue.longitude(), CREATED_BY, CREATED_BY);
            double min = venue.courts().stream().mapToDouble(Court::price).min().orElse(0);
            double max = venue.courts().stream().mapToDouble(Court::price).max().orElse(0);
            inserted += jdbc.update("""
                            insert into venues (venue_id, name, description, open_time, close_time, active, min_price,
                                                max_price, average_rating, total_reviews, phone, email, owner_id,
                                                address_id, amenities, created_at, created_by, updated_at, updated_by)
                            values (?, ?, ?, ?, ?, true, ?, ?, 0, 0, ?, ?, ?, ?, cast(? as jsonb), ?, ?, now(), ?)
                            on conflict do nothing
                            """, venue.id(), venue.name(), description(venue), Time.valueOf(venue.open()),
                    Time.valueOf(venue.close()), min, max, "0283" + String.format("%07d", Math.abs(venue.id().hashCode()) % 10_000_000),
                    DemoPopulation.ascii(venue.name()) + "@" + DemoPopulation.DOMAIN, venue.owner().userId(), addressId,
                    toJson(venue.amenities()), since, CREATED_BY, CREATED_BY);
            for (Court court : venue.courts()) {
                jdbc.update("""
                                insert into venue_courts (venue_court_id, name, sport_type, capacity, surface_type, active,
                                                          venue_id, created_at, created_by, updated_at, updated_by)
                                values (?, ?, ?, ?, ?, true, ?, ?, ?, now(), ?) on conflict do nothing
                                """, court.id(), court.name(), court.sport(), court.capacity(), court.surface(),
                        venue.id(), since, CREATED_BY, CREATED_BY);
            }
            jdbc.update("""
                            insert into cancellation_policies (policy_id, full_refund_hours_before,
                                                               partial_refund_hours_before, partial_refund_percentage,
                                                               no_refund_hours_before, venue_id,
                                                               created_at, created_by, updated_at, updated_by)
                            select ?, 24, 6, 50, 2, ?, now(), ?, now(), ?
                            where not exists (select 1 from cancellation_policies where venue_id = ?)
                            """, id("sim-policy", venue.id().toString()), venue.id(), CREATED_BY, CREATED_BY, venue.id());
        }
        // Chủ sân chỉ mở được khu vận hành khi có hồ sơ đã duyệt (gắn với cơ sở đầu tiên của họ).
        Set<UUID> approved = new HashSet<>();
        for (Venue venue : venues) {
            if (!approved.add(venue.owner().userId())) continue;
            // address_id là duy nhất ở cả venues lẫn owner_applications: hồ sơ giữ bản sao địa chỉ riêng.
            UUID applicationAddressId = id("sim-owner-application-address", venue.owner().email());
            jdbc.update("""
                            insert into addresses (address_id, address, ward, district, city, latitude, longitude,
                                                   created_at, created_by, updated_at, updated_by)
                            select ?, address, ward, district, city, latitude, longitude, now(), ?, now(), ?
                            from addresses where address_id = ? on conflict do nothing
                            """, applicationAddressId, CREATED_BY, CREATED_BY, id("sim-address", venue.id().toString()));
            jdbc.update("""
                            insert into owner_applications (owner_application_id, user_id, full_name, phone, email,
                                                            business_name, business_type, status, address_id, venue_id,
                                                            reviewed_at, received_at, viewed_at,
                                                            created_at, created_by, updated_at, updated_by)
                            select ?, ?, ?, '0909000000', ?, ?, 'INDIVIDUAL', 'APPROVED', ?, ?, now(), now(), now(),
                                   now(), ?, now(), ?
                            where not exists (select 1 from owner_applications where user_id = ? and status = 'APPROVED')
                            """, id("sim-owner-application", venue.owner().email()), venue.owner().userId(),
                    venue.owner().fullName(), venue.owner().email(), venue.name(),
                    applicationAddressId, venue.id(), CREATED_BY, CREATED_BY, venue.owner().userId());
        }
        return inserted;
    }

    /** Khung giờ theo giờ mở cửa, {@link #SLOT_DAYS} ngày tới; mỗi sân con một câu lệnh, id tất định theo md5. */
    private int seedSlots(List<Venue> venues, LocalDate today) {
        int inserted = 0;
        for (Venue venue : venues) {
            for (Court court : venue.courts()) {
                inserted += jdbc.update("""
                                insert into time_slots (time_slot_id, date, start_time, end_time, price_per_hour, status,
                                                        venue_court_id, created_at, created_by, updated_at, updated_by)
                                select md5('goatsports:sim-slot:' || ? || '|' || d::date || '|' || h)::uuid,
                                       d::date, make_time(h, 0, 0), make_time(h + 1, 0, 0),
                                       case when h between 17 and 20 then round(? * 1.4 / 1000) * 1000 else ? end,
                                       'AVAILABLE', ?, now(), ?, now(), ?
                                from generate_series(cast(? as date), cast(? as date), interval '1 day') d,
                                     generate_series(?, ?) h
                                on conflict do nothing
                                """, court.id().toString(), court.price(), court.price(), court.id(), CREATED_BY,
                        CREATED_BY, Date.valueOf(today), Date.valueOf(today.plusDays(SLOT_DAYS - 1)),
                        venue.open().getHour(), venue.close().getHour() - 1);
            }
        }
        return inserted;
    }

    record BookingStats(int inserted, int reviews, LocalDate from, LocalDate to) { }

    private BookingStats seedBookings(List<Venue> venues, LocalDate today) {
        LocalDate first = today.minusDays(DemoPopulation.HISTORY_DAYS);
        LocalDate last = today.plusDays(FUTURE_DAYS);
        Date latest = jdbc.queryForObject("select max(play_date) from bookings where created_by = ?", Date.class, CREATED_BY);
        LocalDate from = latest == null ? first : latest.toLocalDate().plusDays(1);
        if (from.isBefore(first)) from = first;
        if (from.isAfter(last)) return new BookingStats(0, 0, from, last);

        List<DemoPopulation.Player> players = DemoPopulation.players();
        List<Object[]> bookings = new ArrayList<>();
        List<Object[]> checkIns = new ArrayList<>();
        List<Object[]> reviews = new ArrayList<>();
        // Người dùng thật có thể đã đặt khung giờ sắp tới trên sân mô phỏng: không đặt chồng lên.
        Set<String> taken = new HashSet<>(jdbc.queryForList("""
                select b.venue_court_id || '|' || b.play_date || '|' || extract(hour from b.start_time)::int
                from bookings b join venue_courts c on c.venue_court_id = b.venue_court_id
                where c.created_by = ? and b.play_date >= ? and b.status not in ('CANCELLED', 'EXPIRED', 'REFUNDED')
                """, String.class, CREATED_BY, Date.valueOf(from)));
        for (LocalDate date = from; !date.isAfter(last); date = date.plusDays(1)) {
            for (DemoPopulation.Player player : players) {
                Random random = DemoPopulation.random(0xB00C, date.toEpochDay(), player.index());
                if (random.nextDouble() >= dailyProbability(player, date, first, today)) continue;
                String sport = player.sports().size() > 1 && random.nextInt(100) < 25 ? player.sports().get(1) : player.sports().get(0);
                List<Venue> candidates = venues.stream()
                        .filter(venue -> venue.city() == player.city() && venue.offers(sport)).toList();
                if (candidates.isEmpty()) continue;
                // Mỗi người có một sân "quen" cho mỗi môn; 65% số lần đặt ở đó.
                Venue venue = random.nextInt(100) < 65
                        ? candidates.get(Math.floorMod((player.email() + sport).hashCode(), candidates.size()))
                        : candidates.get(random.nextInt(candidates.size()));
                int hour = hour(random, date, venue);
                String slotKey = "|" + date + "|" + hour;
                Court court = venue.courts().stream()
                        .filter(item -> item.sport().equals(sport) && !taken.contains(item.id() + slotKey))
                        .findFirst().orElse(null);
                if (court == null) continue;
                taken.add(court.id() + slotKey);
                String status = !date.isBefore(today) ? "CONFIRMED"
                        : random.nextInt(100) < 88 ? "COMPLETED" : random.nextBoolean() ? "CANCELLED" : "NO_SHOW";
                double total = Math.round((hour >= 17 && hour <= 20 ? court.price() * 1.4 : court.price()) / 1000d) * 1000d;
                double deposit = Math.round(total * 0.3 / 1000d) * 1000d;
                UUID bookingId = id("sim-booking", player.email() + "|" + date + "|" + hour);
                Timestamp createdAt = Timestamp.valueOf(date.minusDays(random.nextInt(5)).atTime(7 + random.nextInt(14), 0));
                bookings.add(new Object[]{bookingId, player.userId(), Date.valueOf(date), Time.valueOf(LocalTime.of(hour, 0)),
                        Time.valueOf(LocalTime.of(hour + 1, 0)), status, total, deposit, total - deposit,
                        "SM" + Math.floorMod(bookingId.getMostSignificantBits(), 1_000_000_000_000L), court.id(),
                        createdAt, CREATED_BY, createdAt, CREATED_BY});
                if (!"COMPLETED".equals(status)) continue;
                checkIns.add(new Object[]{id("sim-checkin", bookingId.toString()), venue.owner().userId(), total - deposit,
                        Timestamp.valueOf(date.atTime(hour, 0).minusMinutes(10)), bookingId});
                if (random.nextInt(100) < 30) {
                    int rating = (int) Math.max(1, Math.min(5, Math.round(venue.quality() + random.nextGaussian() * 0.7)));
                    Timestamp reviewedAt = Timestamp.valueOf(date.plusDays(1).atTime(19 + random.nextInt(3), 0));
                    reviews.add(new Object[]{id("sim-review", bookingId.toString()), player.userId(), rating,
                            review(random, rating, sport), venue.id(), bookingId, reviewedAt, CREATED_BY, reviewedAt, CREATED_BY});
                }
            }
        }
        int inserted = sum(jdbc.batchUpdate("""
                        insert into bookings (booking_id, player_id, play_date, start_time, end_time, status, source,
                                              total_price, deposit_amount, remaining_amount, booking_code, venue_court_id,
                                              created_at, created_by, updated_at, updated_by)
                        values (?, ?, ?, ?, ?, ?, 'DIRECT', ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict do nothing
                        """, bookings, 500, (statement, row) -> bind(statement, row)));
        jdbc.batchUpdate("""
                        insert into check_in_records (check_in_id, checked_in_by, method, remaining_amount_collected,
                                                      checked_in_at, booking_id, created_at, created_by, updated_at, updated_by)
                        values (?, ?, 'BOOKING_CODE', ?, ?, ?, now(), 'demo-sim', now(), 'demo-sim') on conflict do nothing
                        """, checkIns, 500, (statement, row) -> bind(statement, row));
        int reviewed = sum(jdbc.batchUpdate("""
                        insert into reviews (review_id, player_id, rating, content, status, venue_id, booking_id,
                                             created_at, created_by, updated_at, updated_by)
                        values (?, ?, ?, ?, 'PUBLISHED', ?, ?, ?, ?, ?, ?) on conflict do nothing
                        """, reviews, 500, (statement, row) -> bind(statement, row)));
        // Lượt đặt sắp tới giữ đúng khung giờ, nếu không sân bị chào bán hai lần.
        jdbc.update("""
                update time_slots t set status = 'BOOKED', updated_at = now(), updated_by = ?
                from bookings b
                where b.created_by = ? and b.status = 'CONFIRMED' and t.venue_court_id = b.venue_court_id
                  and t.date = b.play_date and t.start_time = b.start_time and t.status = 'AVAILABLE'
                """, CREATED_BY, CREATED_BY);
        return new BookingStats(inserted, reviewed, from, last);
    }

    /** Xác suất người chơi đặt sân trong một ngày: theo mức năng nổ, cuối tuần đông hơn, cộng đồng lớn dần theo thời gian. */
    static double dailyProbability(DemoPopulation.Player player, LocalDate date, LocalDate first, LocalDate today) {
        double progress = Math.min(1, Math.max(0, (date.toEpochDay() - first.toEpochDay()) / (double) (today.toEpochDay() - first.toEpochDay())));
        double growth = 0.55 + 0.65 * progress;
        double weekday = date.getDayOfWeek().getValue() >= 6 ? 1.6 : 0.75;
        return Math.min(0.9, player.bookingsPerWeek() / 7 * weekday * growth);
    }

    /** Giờ bắt đầu: tối là giờ vàng; cuối tuần có thêm buổi sáng; luôn nằm trong giờ mở cửa. */
    private static int hour(Random random, LocalDate date, Venue venue) {
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        int[] hours = weekend ? new int[]{6, 7, 8, 9, 15, 16, 17, 18, 19, 20} : new int[]{6, 11, 17, 18, 19, 20, 21};
        int[] weights = weekend ? new int[]{8, 12, 10, 6, 6, 8, 12, 14, 14, 10} : new int[]{6, 4, 14, 22, 24, 18, 8};
        int hour = hours[DemoPopulation.weighted(random, weights)];
        return Math.max(venue.open().getHour(), Math.min(venue.close().getHour() - 1, hour));
    }

    private static final String[] GOOD = {"Sân đẹp, mặt sân êm, chủ sân nhiệt tình. Sẽ quay lại.",
            "Đặt online nhanh, tới nơi check-in bằng mã là vào chơi luôn.", "Đèn sáng, giá hợp lý so với khu vực.",
            "Nhóm mình chơi ở đây mỗi tuần, chất lượng ổn định.", "Có chỗ gửi xe rộng, nước uống sẵn, sạch sẽ."};
    private static final String[] MID = {"Sân ổn nhưng giờ cao điểm hơi đông, nên đặt sớm.",
            "Chất lượng tạm được, phòng thay đồ cần sạch hơn.", "Giá hơi cao so với mặt sân, bù lại vị trí thuận tiện."};
    private static final String[] BAD = {"Mặt sân xuống cấp, đèn một góc bị tối.", "Tới đúng giờ nhưng phải chờ 15 phút mới vào sân."};

    private static String review(Random random, int rating, String sport) {
        String[] pool = rating >= 4 ? GOOD : rating == 3 ? MID : BAD;
        String text = pool[random.nextInt(pool.length)];
        return random.nextInt(100) < 40 ? text + " (" + sportLabel(sport) + ")" : text;
    }

    private static String description(Venue venue) {
        String sports = venue.courts().stream().map(Court::sport).distinct().map(DemoSimulationSeeder::sportLabel)
                .reduce((left, right) -> left + ", " + right).orElse("");
        return "Cụm " + venue.courts().size() + " sân " + sports.toLowerCase() + " tại " + venue.district() + ", "
                + venue.city().name() + ". Mở cửa " + venue.open() + " – " + venue.close() + ".";
    }

    private static String sportLabel(String sport) {
        return switch (sport) {
            case "FOOTBALL" -> "Bóng đá";
            case "BADMINTON" -> "Cầu lông";
            case "PICKLEBALL" -> "Pickleball";
            case "TENNIS" -> "Tennis";
            case "BASKETBALL" -> "Bóng rổ";
            default -> "Bóng chuyền";
        };
    }

    private void refreshRatings() {
        jdbc.update("""
                update venues v set average_rating = coalesce(stat.avg_rating, 0), total_reviews = coalesce(stat.total, 0)
                from (select venue_id, round(avg(rating)::numeric, 1) as avg_rating, count(*) as total
                      from reviews where status = 'PUBLISHED' group by venue_id) stat
                where v.venue_id = stat.venue_id
                """);
    }

    private static void bind(java.sql.PreparedStatement statement, Object[] row) throws java.sql.SQLException {
        for (int column = 0; column < row.length; column++) statement.setObject(column + 1, row[column]);
    }

    private static int sum(int[][] batches) {
        int total = 0;
        for (int[] batch : batches) for (int count : batch) total += Math.max(count, 0);
        return total;
    }

    private static String toJson(List<String> values) {
        return values.stream().map(value -> "\"" + value.replace("\"", "") + "\"")
                .reduce((left, right) -> left + "," + right).map(body -> "[" + body + "]").orElse("[]");
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }

    static UUID id(String kind, String key) {
        return UUID.nameUUIDFromBytes(("goatsports:" + kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }
}
