package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.request.venue.AdminVenueFilter;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.request.venue.OwnerReviewFilter;
import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.CheckInRecordRepository;
import iuh.fit.venueservice.application.port.persistence.ReviewRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Danh sách phân trang không được sinh truy vấn theo từng dòng (N+1). BookingEntity (hủy / check-in / đánh giá) và
 * VenueEntity (chính sách hủy) có quan hệ @OneToOne(mappedBy) không lazy được; thiếu entity graph thì 10 lượt đặt
 * = 1 + 1 + 30 truy vấn.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class BookingListQueryCountTest {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private CheckInRecordRepository checkInRecordRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void pagedListsLoadInAFixedNumberOfQueries() {
        UUID ownerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID firstVenueId = null;
        // 5 cơ sở x 2 lượt: lượt thứ 3k bị hủy, còn lại đã check-in và có đánh giá.
        for (int venueIndex = 0; venueIndex < 5; venueIndex++) {
            UUID addressId = UUID.randomUUID();
            UUID venueId = UUID.randomUUID();
            UUID courtId = UUID.randomUUID();
            if (firstVenueId == null) firstVenueId = venueId;
            jdbc.update("insert into addresses (address_id, address) values (?, 'Q1')", addressId);
            jdbc.update("insert into venues (venue_id, name, owner_id, address_id, active) values (?, ?, ?, ?, true)",
                    venueId, "QC venue " + venueIndex, ownerId, addressId);
            jdbc.update("insert into venue_courts (venue_court_id, name, sport_type, venue_id, active) values (?, 'A', 'BADMINTON', ?, true)",
                    courtId, venueId);
            jdbc.update("""
                    insert into cancellation_policies (policy_id, venue_id, full_refund_hours_before,
                                                       partial_refund_hours_before, no_refund_hours_before,
                                                       partial_refund_percentage)
                    values (?, ?, 24, 6, 2, 50)
                    """, UUID.randomUUID(), venueId);
            for (int slot = 0; slot < 2; slot++) {
                int index = venueIndex * 2 + slot;
                UUID bookingId = UUID.randomUUID();
                jdbc.update("""
                        insert into bookings (booking_id, player_id, play_date, start_time, end_time, status, source,
                                              total_price, deposit_amount, remaining_amount, booking_code, venue_court_id, created_at)
                        values (?, ?, ?, '18:00', '19:00', 'COMPLETED', 'DIRECT', 100000, 30000, 70000, ?, ?, current_timestamp)
                        """, bookingId, playerId, LocalDate.of(2026, 10, 1).plusDays(index), "QC" + index, courtId);
                if (index % 3 == 0) {
                    jdbc.update("""
                            insert into booking_cancellations (cancellation_id, requested_by_user_id, reason, status,
                                                               review_mode, booking_id)
                            values (?, ?, 'x', 'APPROVED', 'AUTOMATIC', ?)
                            """, UUID.randomUUID(), playerId, bookingId);
                } else {
                    jdbc.update("""
                            insert into check_in_records (check_in_id, checked_in_by, method, booking_id, checked_in_at)
                            values (?, ?, 'QR_CODE', ?, current_timestamp)
                            """, UUID.randomUUID(), ownerId, bookingId);
                    jdbc.update("""
                            insert into reviews (review_id, booking_id, player_id, venue_id, rating, status, created_at)
                            values (?, ?, ?, ?, 5, 'PUBLISHED', current_timestamp)
                            """, UUID.randomUUID(), bookingId, playerId, venueId);
                }
            }
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        Map<String, Long> queries = new LinkedHashMap<>();

        statistics.clear();
        assertThat(bookingRepository.findOwnedBookings(
                new OwnerBookingFilter(ownerId, null, null, null, null, null, null, 0, 20)).items()).hasSize(10);
        queries.put("đơn đặt sân (chủ sân)", statistics.getPrepareStatementCount());

        statistics.clear();
        assertThat(bookingRepository.findPlayerPage(playerId, null, 0, 20).items()).hasSize(10);
        queries.put("lượt đặt (người chơi)", statistics.getPrepareStatementCount());

        statistics.clear();
        assertThat(checkInRecordRepository.findOwnedCheckIns(
                new OwnerCheckInFilter(ownerId, null, null, null, 0, 20)).items()).hasSize(6);
        queries.put("lịch sử check-in", statistics.getPrepareStatementCount());

        statistics.clear();
        assertThat(reviewRepository.findOwnedReviews(
                new OwnerReviewFilter(ownerId, null, null, null, null, null, 0, 20)).items()).hasSize(6);
        queries.put("đánh giá (chủ sân)", statistics.getPrepareStatementCount());

        statistics.clear();
        assertThat(reviewRepository.findPublishedByVenueId(firstVenueId, 0, 20).items()).hasSize(1);
        queries.put("đánh giá công khai của cơ sở", statistics.getPrepareStatementCount());

        statistics.clear();
        assertThat(venueRepository.findAll(new AdminVenueFilter(null, "QC venue", 0, 20)).items()).hasSize(5);
        queries.put("cơ sở (admin)", statistics.getPrepareStatementCount());

        statistics.clear();
        assertThat(venueRepository.search(new SearchVenueCommand("QC venue", null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 20, null)).items()).isNotEmpty();
        queries.put("tìm cơ sở", statistics.getPrepareStatementCount());

        // Trang dữ liệu + đếm tổng (nếu cần) + vài truy vấn nạp theo lô — không phụ thuộc số dòng (10 dòng
        // mà N+1 thì ra hàng chục truy vấn).
        assertThat(queries).allSatisfy((list, count) -> assertThat(count).as(list).isLessThanOrEqualTo(3));
    }
}
