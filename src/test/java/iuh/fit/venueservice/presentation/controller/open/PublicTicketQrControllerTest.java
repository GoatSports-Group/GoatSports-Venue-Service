package iuh.fit.venueservice.presentation.controller.open;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.infrastructure.security.BookingQrAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicTicketQrControllerTest {
    private final BookingQrAdapter qr = new BookingQrAdapter("test-secret",
            Clock.fixed(Instant.parse("2026-11-04T05:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
    private final PublicTicketQrController controller = new PublicTicketQrController(qr);

    @Test
    void drawsAPngForASignedTicket() throws Exception {
        String code = qr.issue(UUID.randomUUID(), LocalDateTime.of(2026, 11, 5, 7, 0));

        ResponseEntity<byte[]> response = controller.qr(code);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getBody()).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    void refusesArbitraryContent() {
        assertThatThrownBy(() -> controller.qr("https://example.com/phishing"))
                .isInstanceOf(ServiceException.class);
    }
}
