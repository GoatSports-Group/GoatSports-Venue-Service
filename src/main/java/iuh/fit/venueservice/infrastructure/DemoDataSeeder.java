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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                            new CourtSeed("Sân Mỹ Khê 2", SportType.PICKLEBALL, 4, "Mặt nhựa acrylic", 150_000))),

            // Nhà thi đấu cho các giải bóng rổ / bóng chuyền mẫu của club-service.
            new VenueSeed("Nhà Thi Đấu Tân Bình", "bao.tranquoc@goatsports.vn",
                    "Nhà thi đấu đa năng trong nhà: sân bóng rổ sàn gỗ và sân bóng chuyền tiêu chuẩn, có khán đài.",
                    "448 Hoàng Văn Thụ", "Phường 4", "Tân Bình", "Hồ Chí Minh",
                    10.7996, 106.6588, LocalTime.of(6, 0), LocalTime.of(22, 0),
                    "02838112233", "nhathidautanbinh@goatsports.vn",
                    List.of(new CourtSeed("Sân bóng rổ 1", SportType.BASKETBALL, 10, "Sàn gỗ", 350_000),
                            new CourtSeed("Sân bóng rổ 2", SportType.BASKETBALL, 10, "Sàn gỗ", 350_000),
                            new CourtSeed("Sân bóng chuyền 1", SportType.VOLLEYBALL, 12, "Sàn PVC", 300_000)))
    );

    /**
     * Lượt đặt sân mẫu. `daysOffset` âm là đã đá xong, dương là sắp tới.
     * Booking tham chiếu sân con + ngày/giờ chứ không tham chiếu hàng `time_slots`,
     * nên lượt đặt trong quá khứ không cần khung giờ tương ứng còn tồn tại.
     */
    private record BookingSeed(
            String playerEmail, String venueName, String courtName,
            int daysOffset, LocalTime startTime, String status,
            Integer rating, String reviewContent) {}

    private static final List<BookingSeed> BOOKINGS = List.of(
            new BookingSeed("huy.danggia@goatsports.vn", "Sân Thể Thao Bình Thạnh",
                    "Sân số 1 (7 người)", -10, LocalTime.of(19, 0), "COMPLETED",
                    5, "Mặt cỏ mới, đèn sáng, có phòng thay đồ sạch. Nhóm mình đá đều ở đây."),
            new BookingSeed("tung.vuthanh@goatsports.vn", "Sân Thể Thao Bình Thạnh",
                    "Sân số 2 (7 người)", -6, LocalTime.of(20, 0), "COMPLETED",
                    4, "Sân ổn, chỉ hơi kẹt xe giờ tan tầm nên nên đi sớm 15 phút."),
            new BookingSeed("linh.buikhanh@goatsports.vn", "Trung Tâm Cầu Lông Phú Nhuận",
                    "Sân A", -4, LocalTime.of(8, 0), "COMPLETED",
                    5, "Sàn gỗ nảy tốt, máy lạnh mát, nhân viên nhiệt tình."),
            new BookingSeed("anh.ngoduc@goatsports.vn", "Sân Tennis Mỹ Đình",
                    "Sân 1", -3, LocalTime.of(18, 0), "CANCELLED", null, null),
            new BookingSeed("minh.hoangnhat@goatsports.vn", "Sân Thể Thao Bình Thạnh",
                    "Sân số 3 (5 người)", 2, LocalTime.of(19, 0), "CONFIRMED", null, null),
            new BookingSeed("ngoc.caobao@goatsports.vn", "Khu Liên Hợp Pickleball Sơn Trà",
                    "Sân Mỹ Khê 1", 3, LocalTime.of(17, 0), "CONFIRMED", null, null)
    );

    /**
     * Sân giữ cho các giải mẫu của club-service (TournamentShowcaseSeeder). Tên giải là khóa sinh tournamentId,
     * phải trùng từng ký tự với bên club-service. Người chơi thấy các khung giờ này là "Giải đấu" và không đặt được.
     */
    private record CourtHoldSeed(String tournamentName, String venueName, List<String> courtNames,
                                 int fromDay, int toDay, LocalTime dailyStart, LocalTime dailyEnd) {}

    private static final List<CourtHoldSeed> COURT_HOLDS = List.of(
            new CourtHoldSeed("Cúp Sân 7 Bình Thạnh Mùa Thu 2026", "Sân Thể Thao Bình Thạnh",
                    List.of("Sân số 1 (7 người)", "Sân số 2 (7 người)"), -4, 3, LocalTime.of(17, 0), LocalTime.of(22, 0)),
            new CourtHoldSeed("Giải Futsal 5 Người Bình Thạnh 2026", "Sân Thể Thao Bình Thạnh",
                    List.of("Sân số 3 (5 người)"), -8, 1, LocalTime.of(18, 0), LocalTime.of(21, 0)),
            new CourtHoldSeed("Giải Bóng Chuyền Tân Bình Mở Rộng 2026", "Nhà Thi Đấu Tân Bình",
                    List.of("Sân bóng chuyền 1"), 10, 12, LocalTime.of(17, 0), LocalTime.of(21, 0))
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
        int policies = 0;
        for (VenueSeed venue : VENUES) {
            policies += insertCancellationPolicy(venue);
        }
        int bookings = 0;
        int reviews = 0;
        for (BookingSeed booking : BOOKINGS) {
            bookings += insertBooking(booking);
            reviews += insertReview(booking);
        }
        refreshVenueRatings();
        int holds = 0;
        for (CourtHoldSeed hold : COURT_HOLDS) {
            for (String courtName : hold.courtNames()) holds += insertCourtHold(hold, courtName);
        }
        log.info("Dữ liệu mẫu: {} lịch giữ sân cho giải đấu (bỏ qua bản ghi đã có)", holds);
        log.info("Dữ liệu mẫu: {} sân, {} sân con, {} khung giờ, {} chính sách hủy,"
                        + " {} lượt đặt, {} đánh giá (bỏ qua bản ghi đã có)",
                venues, courts, slots, policies, bookings, reviews);
    }

    /**
     * Không có chính sách hủy thì calculateRefundPercentage trả 0% cho mọi lượt hủy,
     * nên luồng hoàn tiền chạy đúng nhưng nhìn như hỏng. Mỗi sân một chính sách.
     */
    private int insertCancellationPolicy(VenueSeed venue) {
        return entityManager.createNativeQuery("""
                        insert into cancellation_policies (policy_id, full_refund_hours_before,
                                                           partial_refund_hours_before, partial_refund_percentage,
                                                           no_refund_hours_before, venue_id,
                                                           created_at, created_by, updated_at, updated_by)
                        select :policyId, 24, 6, 50, 2, :venueId,
                               now(), 'demo-seed', now(), 'demo-seed'
                        where not exists (select 1 from cancellation_policies where venue_id = :venueId)
                        """)
                .setParameter("policyId", id("policy", venue.name()))
                .setParameter("venueId", id("venue", venue.name()))
                .executeUpdate();
    }

    private CourtSeed court(String venueName, String courtName) {
        return VENUES.stream()
                .filter(venue -> venue.name().equals(venueName))
                .flatMap(venue -> venue.courts().stream())
                .filter(court -> court.name().equals(courtName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sân con không có trong dữ liệu mẫu: " + courtName));
    }

    private String ownerOf(String venueName) {
        return VENUES.stream().filter(venue -> venue.name().equals(venueName))
                .findFirst().orElseThrow().ownerEmail();
    }

    private UUID bookingIdOf(BookingSeed seed, LocalDate playDate) {
        return id("booking", seed.playerEmail() + "|" + seed.venueName() + "|" + playDate);
    }

    private int insertBooking(BookingSeed seed) {
        CourtSeed court = court(seed.venueName(), seed.courtName());
        LocalDate playDate = LocalDate.now().plusDays(seed.daysOffset());
        boolean peak = !seed.startTime().isBefore(LocalTime.of(17, 0))
                && seed.startTime().isBefore(LocalTime.of(21, 0));
        double total = Math.round((peak ? court.pricePerHour() * 1.4 : court.pricePerHour()) / 1000d) * 1000d;
        double deposit = Math.round(total * 0.3 / 1000d) * 1000d;
        UUID bookingId = bookingIdOf(seed, playDate);

        int inserted = entityManager.createNativeQuery("""
                        insert into bookings (booking_id, player_id, play_date, start_time, end_time,
                                              status, source, total_price, deposit_amount, remaining_amount,
                                              booking_code, venue_court_id,
                                              created_at, created_by, updated_at, updated_by)
                        select :bookingId, :playerId, :playDate, :startTime, :endTime,
                               :status, 'DIRECT', :total, :deposit, :remaining,
                               :bookingCode, :courtId,
                               :createdAt, 'demo-seed', :createdAt, 'demo-seed'
                        where not exists (select 1 from bookings where booking_id = :bookingId)
                        """)
                .setParameter("bookingId", bookingId)
                .setParameter("playerId", userId(seed.playerEmail()))
                .setParameter("playDate", playDate)
                .setParameter("startTime", seed.startTime())
                .setParameter("endTime", seed.startTime().plusHours(1))
                .setParameter("status", seed.status())
                .setParameter("total", total)
                .setParameter("deposit", deposit)
                .setParameter("remaining", total - deposit)
                .setParameter("bookingCode", bookingCode(bookingId))
                .setParameter("courtId", id("court", seed.venueName() + "|" + seed.courtName()))
                .setParameter("createdAt", playDate.minusDays(2).atTime(9, 0))
                .executeUpdate();

        if ("COMPLETED".equals(seed.status())) {
            insertCheckIn(seed, bookingId, playDate, total - deposit);
        }
        if ("CONFIRMED".equals(seed.status())) {
            markSlotBooked(seed, playDate);
        }
        return inserted;
    }

    /** Mã đặt sân người dùng đọc được, suy ra từ id nên chạy lại vẫn ra cùng một mã. */
    private String bookingCode(UUID bookingId) {
        return "GS" + Math.abs(bookingId.getMostSignificantBits() % 100_000_000L);
    }

    private void insertCheckIn(BookingSeed seed, UUID bookingId, LocalDate playDate, double remaining) {
        entityManager.createNativeQuery("""
                        insert into check_in_records (check_in_id, checked_in_by, method,
                                                      remaining_amount_collected, checked_in_at, booking_id,
                                                      created_at, created_by, updated_at, updated_by)
                        select :checkInId, :checkedInBy, 'BOOKING_CODE',
                               :remaining, :checkedInAt, :bookingId,
                               now(), 'demo-seed', now(), 'demo-seed'
                        where not exists (select 1 from check_in_records where booking_id = :bookingId)
                        """)
                .setParameter("checkInId", id("checkin", bookingId.toString()))
                // Chu san moi la nguoi quet ma, khong phai nguoi choi.
                .setParameter("checkedInBy", userId(ownerOf(seed.venueName())))
                .setParameter("remaining", BigDecimal.valueOf(remaining))
                .setParameter("checkedInAt", playDate.atTime(seed.startTime().minusMinutes(10)))
                .setParameter("bookingId", bookingId)
                .executeUpdate();
    }

    /** Lượt đặt sắp tới phải khoá khung giờ tương ứng, nếu không sân bị chào bán hai lần. */
    private void markSlotBooked(BookingSeed seed, LocalDate playDate) {
        entityManager.createNativeQuery("""
                        update time_slots set status = 'BOOKED', updated_at = now(), updated_by = 'demo-seed'
                        where venue_court_id = :courtId and date = :playDate
                          and start_time = :startTime and status = 'AVAILABLE'
                        """)
                .setParameter("courtId", id("court", seed.venueName() + "|" + seed.courtName()))
                .setParameter("playDate", playDate)
                .setParameter("startTime", seed.startTime())
                .executeUpdate();
    }

    private int insertReview(BookingSeed seed) {
        if (seed.rating() == null) return 0;
        LocalDate playDate = LocalDate.now().plusDays(seed.daysOffset());
        UUID bookingId = bookingIdOf(seed, playDate);
        return entityManager.createNativeQuery("""
                        insert into reviews (review_id, player_id, rating, content, status,
                                             venue_id, booking_id,
                                             created_at, created_by, updated_at, updated_by)
                        select :reviewId, :playerId, :rating, :content, 'PUBLISHED',
                               :venueId, :bookingId,
                               :createdAt, 'demo-seed', :createdAt, 'demo-seed'
                        where not exists (select 1 from reviews where booking_id = :bookingId)
                        """)
                .setParameter("reviewId", id("review", bookingId.toString()))
                .setParameter("playerId", userId(seed.playerEmail()))
                .setParameter("rating", seed.rating())
                .setParameter("content", seed.reviewContent())
                .setParameter("venueId", id("venue", seed.venueName()))
                .setParameter("bookingId", bookingId)
                .setParameter("createdAt", playDate.plusDays(1).atTime(20, 0))
                .executeUpdate();
    }

    /** Tính lại điểm trung bình từ review thật thay vì gán số cứng. */
    private void refreshVenueRatings() {
        entityManager.createNativeQuery("""
                        update venues v set
                            average_rating = coalesce(stat.avg_rating, 0),
                            total_reviews = coalesce(stat.total, 0),
                            updated_at = now(), updated_by = 'demo-seed'
                        from (
                            select venue_id, round(avg(rating)::numeric, 1) as avg_rating, count(*) as total
                            from reviews where status = 'PUBLISHED' group by venue_id
                        ) stat
                        where v.venue_id = stat.venue_id
                        """)
                .executeUpdate();
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

    private int insertCourtHold(CourtHoldSeed hold, String courtName) {
        UUID tournamentId = id("tournament", hold.tournamentName());
        UUID courtId = id("court", hold.venueName() + "|" + courtName);
        return entityManager.createNativeQuery("""
                        insert into court_holds (hold_id, tournament_id, venue_id, venue_court_id,
                                                 from_date, to_date, daily_start, daily_end, created_at)
                        select :holdId, :tournamentId, :venueId, :courtId, :fromDate, :toDate, :dailyStart, :dailyEnd, now()
                        where not exists (select 1 from court_holds where hold_id = :holdId)
                        """)
                .setParameter("holdId", id("court-hold", tournamentId + "|" + courtId))
                .setParameter("tournamentId", tournamentId)
                .setParameter("venueId", id("venue", hold.venueName()))
                .setParameter("courtId", courtId)
                .setParameter("fromDate", LocalDate.now().plusDays(hold.fromDay()))
                .setParameter("toDate", LocalDate.now().plusDays(hold.toDay()))
                .setParameter("dailyStart", hold.dailyStart())
                .setParameter("dailyEnd", hold.dailyEnd())
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
