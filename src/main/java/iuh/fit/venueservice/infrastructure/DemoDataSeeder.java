package iuh.fit.venueservice.infrastructure;

import iuh.fit.venueservice.domain.enums.SportType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu mẫu cho môi trường dev. Bật bằng `app.demo-data.enabled=true`.
 *
 * <p>Chủ sân tham chiếu sang auth-service bằng **cùng một quy ước sinh id**:
 * {@code UUID.nameUUIDFromBytes("goatsports:user:" + email)}. Hai service không gọi nhau
 * lúc seed nhưng vẫn ra đúng một UUID, nên `venues.owner_id` luôn khớp `users.user_id`.
 *
 * <p>Native insert vì khoá chính phải giữ nguyên, và `where not exists` cho phép chạy lại nhiều lần.
 */
@Slf4j
@Component
@Order(20)
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    /** Phải trùng quy ước của auth-service, nếu lệch thì sân sẽ không có chủ. */
    private static UUID userId(String email) {
        return UUID.nameUUIDFromBytes(("goatsports:user:" + email).getBytes(StandardCharsets.UTF_8));
    }

    private static UUID id(String kind, String key) {
        return UUID.nameUUIDFromBytes(("goatsports:" + kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private record CourtSeed(String name, SportType sportType, int capacity,
                             String surface, double pricePerHour) {}

    private record VenueSeed(
            String name, String ownerEmail, String description,
            String street, String ward, String district, String city,
            double latitude, double longitude,
            LocalTime openTime, LocalTime closeTime,
            String phone, String email, List<CourtSeed> courts) {}

    private static final List<VenueSeed> VENUES = List.of(
            new VenueSeed("Sân Thể Thao Bình Thạnh", "bao.tranquoc@goatsports.vn",
                    "Cụm sân cỏ nhân tạo 5 và 7 người, có đèn chiếu sáng và phòng thay đồ.",
                    "121 Đinh Bộ Lĩnh", "Phường 26", "Bình Thạnh", "Hồ Chí Minh",
                    10.8039, 106.7118, LocalTime.of(5, 30), LocalTime.of(22, 30),
                    "02835123478", "sanbinhthanh@goatsports.vn",
                    List.of(new CourtSeed("Sân số 1 (7 người)", SportType.FOOTBALL, 14, "Cỏ nhân tạo", 450_000),
                            new CourtSeed("Sân số 2 (7 người)", SportType.FOOTBALL, 14, "Cỏ nhân tạo", 450_000),
                            new CourtSeed("Sân số 3 (5 người)", SportType.FOOTBALL, 10, "Cỏ nhân tạo", 320_000))),

            new VenueSeed("Trung Tâm Cầu Lông Phú Nhuận", "huong.nguyenthimai@goatsports.vn",
                    "Nhà thi đấu trong nhà, sàn gỗ tiêu chuẩn thi đấu, máy lạnh.",
                    "45 Hoàng Văn Thụ", "Phường 15", "Phú Nhuận", "Hồ Chí Minh",
                    10.7995, 106.6702, LocalTime.of(6, 0), LocalTime.of(22, 0),
                    "02838445612", "caulongphunhuan@goatsports.vn",
                    List.of(new CourtSeed("Sân A", SportType.BADMINTON, 4, "Sàn gỗ", 130_000),
                            new CourtSeed("Sân B", SportType.BADMINTON, 4, "Sàn gỗ", 130_000),
                            new CourtSeed("Sân C", SportType.BADMINTON, 4, "Thảm PVC", 110_000))),

            new VenueSeed("Sân Tennis Mỹ Đình", "phuc.lehoang@goatsports.vn",
                    "Bốn sân tennis mặt cứng ngoài trời, có khán đài nhỏ và khu gửi xe rộng.",
                    "Đường Lê Đức Thọ", "Mỹ Đình 2", "Nam Từ Liêm", "Hà Nội",
                    21.0208, 105.7631, LocalTime.of(5, 0), LocalTime.of(21, 30),
                    "02437856249", "tennismydinh@goatsports.vn",
                    List.of(new CourtSeed("Sân 1", SportType.TENNIS, 4, "Mặt cứng", 200_000),
                            new CourtSeed("Sân 2", SportType.TENNIS, 4, "Mặt cứng", 200_000))),

            new VenueSeed("Khu Liên Hợp Pickleball Sơn Trà", "khoi.phamminh@goatsports.vn",
                    "Sân pickleball tiêu chuẩn, cho thuê vợt và bóng, sát biển Mỹ Khê.",
                    "218 Ngô Quyền", "An Hải Bắc", "Sơn Trà", "Đà Nẵng",
                    16.0756, 108.2288, LocalTime.of(6, 0), LocalTime.of(21, 0),
                    "02363892157", "pickleballsontra@goatsports.vn",
                    List.of(new CourtSeed("Sân Mỹ Khê 1", SportType.PICKLEBALL, 4, "Mặt nhựa acrylic", 150_000),
                            new CourtSeed("Sân Mỹ Khê 2", SportType.PICKLEBALL, 4, "Mặt nhựa acrylic", 150_000)))
    );

    /** Số ngày mở khung giờ tính từ hôm nay. */
    private static final int SLOT_DAYS = 7;

    @Override
    @Transactional
    public void run(String... args) {
        int venues = 0;
        int courts = 0;
        int slots = 0;
        for (VenueSeed venue : VENUES) {
            venues += insertVenue(venue);
            for (CourtSeed court : venue.courts()) {
                courts += insertCourt(venue, court);
                slots += insertSlots(venue, court);
            }
        }
        log.info("Dữ liệu mẫu: thêm {} sân, {} sân con và {} khung giờ (bỏ qua bản ghi đã có)",
                venues, courts, slots);
    }

    private int insertVenue(VenueSeed seed) {
        UUID addressId = id("address", seed.name());
        UUID venueId = id("venue", seed.name());

        entityManager.createNativeQuery("""
                        insert into addresses (address_id, address, ward, district, city, latitude, longitude,
                                               created_at, created_by, updated_at, updated_by)
                        select :addressId, :address, :ward, :district, :city, :latitude, :longitude,
                               now(), 'demo-seed', now(), 'demo-seed'
                        where not exists (select 1 from addresses where address_id = :addressId)
                        """)
                .setParameter("addressId", addressId)
                .setParameter("address", seed.street())
                .setParameter("ward", seed.ward())
                .setParameter("district", seed.district())
                .setParameter("city", seed.city())
                .setParameter("latitude", seed.latitude())
                .setParameter("longitude", seed.longitude())
                .executeUpdate();

        double minPrice = seed.courts().stream().mapToDouble(CourtSeed::pricePerHour).min().orElse(0);
        double maxPrice = seed.courts().stream().mapToDouble(CourtSeed::pricePerHour).max().orElse(0);

        return entityManager.createNativeQuery("""
                        insert into venues (venue_id, name, description, open_time, close_time, active,
                                            min_price, max_price, average_rating, total_reviews,
                                            phone, email, owner_id, address_id,
                                            created_at, created_by, updated_at, updated_by)
                        select :venueId, :name, :description, :openTime, :closeTime, true,
                               :minPrice, :maxPrice, 0, 0,
                               :phone, :email, :ownerId, :addressId,
                               now(), 'demo-seed', now(), 'demo-seed'
                        where not exists (select 1 from venues where venue_id = :venueId)
                        """)
                .setParameter("venueId", venueId)
                .setParameter("name", seed.name())
                .setParameter("description", seed.description())
                .setParameter("openTime", seed.openTime())
                .setParameter("closeTime", seed.closeTime())
                .setParameter("minPrice", minPrice)
                .setParameter("maxPrice", maxPrice)
                .setParameter("phone", seed.phone())
                .setParameter("email", seed.email())
                .setParameter("ownerId", userId(seed.ownerEmail()))
                .setParameter("addressId", addressId)
                .executeUpdate();
    }

    private int insertCourt(VenueSeed venue, CourtSeed court) {
        return entityManager.createNativeQuery("""
                        insert into venue_courts (venue_court_id, name, sport_type, capacity, surface_type,
                                                  active, venue_id,
                                                  created_at, created_by, updated_at, updated_by)
                        select :courtId, :name, :sportType, :capacity, :surface,
                               true, :venueId,
                               now(), 'demo-seed', now(), 'demo-seed'
                        where not exists (select 1 from venue_courts where venue_court_id = :courtId)
                        """)
                .setParameter("courtId", id("court", venue.name() + "|" + court.name()))
                .setParameter("name", court.name())
                .setParameter("sportType", court.sportType().name())
                .setParameter("capacity", court.capacity())
                .setParameter("surface", court.surface())
                .setParameter("venueId", id("venue", venue.name()))
                .executeUpdate();
    }

    /**
     * Khung giờ mỗi tiếng từ giờ mở tới giờ đóng, cho {@link #SLOT_DAYS} ngày tới.
     * Khung 17h–21h tính giá cao điểm +40%, đúng thói quen đặt sân buổi tối.
     */
    private int insertSlots(VenueSeed venue, CourtSeed court) {
        UUID courtId = id("court", venue.name() + "|" + court.name());
        int inserted = 0;
        for (int day = 0; day < SLOT_DAYS; day++) {
            LocalDate date = LocalDate.now().plusDays(day);
            LocalTime start = venue.openTime().withMinute(0);
            while (!start.plusHours(1).isAfter(venue.closeTime())) {
                boolean peak = !start.isBefore(LocalTime.of(17, 0)) && start.isBefore(LocalTime.of(21, 0));
                double price = peak ? court.pricePerHour() * 1.4 : court.pricePerHour();
                inserted += entityManager.createNativeQuery("""
                                insert into time_slots (time_slot_id, date, start_time, end_time,
                                                        price_per_hour, status, venue_court_id,
                                                        created_at, created_by, updated_at, updated_by)
                                select :slotId, :date, :startTime, :endTime,
                                       :price, 'AVAILABLE', :courtId,
                                       now(), 'demo-seed', now(), 'demo-seed'
                                where not exists (select 1 from time_slots where time_slot_id = :slotId)
                                """)
                        .setParameter("slotId", id("slot", courtId + "|" + date + "|" + start))
                        .setParameter("date", date)
                        .setParameter("startTime", start)
                        .setParameter("endTime", start.plusHours(1))
                        .setParameter("price", Math.round(price / 1000d) * 1000d)
                        .setParameter("courtId", courtId)
                        .executeUpdate();
                start = start.plusHours(1);
            }
        }
        return inserted;
    }
}
