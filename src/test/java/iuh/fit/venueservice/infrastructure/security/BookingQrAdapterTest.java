package iuh.fit.venueservice.infrastructure.security;

import iuh.fit.venueservice.application.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingQrAdapterTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-30T01:00:00Z"), ZONE);
    private final BookingQrAdapter adapter = new BookingQrAdapter(
            "test-secret-long-enough-for-hmac", clock
    );

    @Test
    void issuesAndVerifiesVersionedSignedToken() throws Exception {
        UUID bookingId = UUID.randomUUID();
        String token = adapter.issue(bookingId, LocalDateTime.of(2026, 8, 30, 10, 0));

        assertThat(token).startsWith("GSQR.v1.");
        assertThat(adapter.verify(token)).isEqualTo(bookingId);
    }

    @Test
    void rejectsTamperedAndExpiredToken() throws Exception {
        UUID bookingId = UUID.randomUUID();
        String valid = adapter.issue(bookingId, LocalDateTime.of(2026, 8, 30, 10, 0));
        String tampered = valid.replace(bookingId.toString(), UUID.randomUUID().toString());

        assertThatThrownBy(() -> adapter.verify(tampered))
                .isInstanceOf(ServiceException.class).hasMessageContaining("không hợp lệ");
        String expired = adapter.issue(bookingId, LocalDateTime.of(2026, 8, 30, 7, 0));
        assertThatThrownBy(() -> adapter.verify(expired))
                .isInstanceOf(ServiceException.class).hasMessageContaining("hết hạn");
    }
}
