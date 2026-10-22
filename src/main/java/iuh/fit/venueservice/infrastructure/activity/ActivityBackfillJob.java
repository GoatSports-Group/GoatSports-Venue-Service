package iuh.fit.venueservice.infrastructure.activity;

import iuh.fit.venueservice.application.port.ActivityEventPublisher;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.BookingEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.BookingEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Nạp bù một lần: các đơn đặt sân có từ trước khi có gợi ý cá nhân hóa được phát sang ai-service, để người dùng cũ
 * có gợi ý ngay chứ không chờ tích lũy lại từ đầu. Chỉ đánh dấu xong khi Kafka đã nhận hết; Kafka chưa lên thì lượt
 * sau thử lại. Phát lại vô hại: ai-service ghi đè theo mã đơn.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "activity.events.enabled", havingValue = "true")
public class ActivityBackfillJob {
    static final String BOOKINGS = "bookings-v1";
    private static final int PAGE_SIZE = 200;
    private static final EnumSet<BookingStatus> BOOKED =
            EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED);

    private final JpaBookingRepository bookingRepository;
    private final BookingEntityMapper bookingMapper;
    private final BookingActivityRecorder recorder;
    private final ActivityEventKafkaPublisher publisher;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(initialDelayString = "${activity.backfill.delay-ms:90000}",
            fixedDelayString = "${activity.backfill.retry-ms:600000}")
    public void backfillBookings() {
        if (isDone(BOOKINGS)) return;
        int sent = 0;
        try {
            for (int page = 0; ; page++) {
                int index = page;
                List<CompletableFuture<?>> pending = new ArrayList<>();
                Page<?> batch = transactionTemplate.execute(status -> {
                    Page<BookingEntity> rows = bookingRepository.findByStatusIn(BOOKED,
                            PageRequest.of(index, PAGE_SIZE, Sort.by("bookingId")));
                    rows.forEach(entity -> {
                        Booking booking = bookingMapper.toDomain(entity);
                        recorder.payloadFor(booking).ifPresent(payload -> pending.add(
                                publisher.send(ActivityEventPublisher.BOOKING_STATE, booking.getPlayerId(), payload,
                                        booking.getCreatedAt() == null ? Instant.now()
                                                : booking.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())));
                    });
                    return rows;
                });
                CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new)).get(30, TimeUnit.SECONDS);
                sent += pending.size();
                if (batch == null || !batch.hasNext()) break;
            }
        } catch (Exception exception) {
            log.info("Activity backfill paused after {} bookings, retrying later: {}", sent, exception.getMessage());
            return;
        }
        jdbc.update("insert into activity_backfills (name, completed_at, event_count) values (?, ?, ?)",
                BOOKINGS, LocalDateTime.now(), sent);
        log.info("Activity backfill done: {} bookings", sent);
    }

    private boolean isDone(String name) {
        Integer count = jdbc.queryForObject("select count(*) from activity_backfills where name = ?", Integer.class, name);
        return count != null && count > 0;
    }
}
