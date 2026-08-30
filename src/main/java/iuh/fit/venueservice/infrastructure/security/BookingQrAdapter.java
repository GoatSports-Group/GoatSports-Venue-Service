package iuh.fit.venueservice.infrastructure.security;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;

@Component
public class BookingQrAdapter implements BookingQrPort {
    private static final String PREFIX = "GSQR";
    private static final String VERSION = "v1";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final byte[] secret;
    private final Clock clock;

    public BookingQrAdapter(
            @Value("${app.check-in.qr-secret}") String secret,
            Clock clock
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    @Override
    public String issue(UUID bookingId, LocalDateTime expiresAt) throws ServiceException {
        if (bookingId == null || expiresAt == null) {
            throw new ServiceException("Booking và thời điểm hết hạn QR là bắt buộc.");
        }
        long expiry = expiresAt.atZone(BUSINESS_ZONE).toEpochSecond();
        String payload = VERSION + "." + bookingId + "." + expiry;
        return PREFIX + "." + payload + "." + sign(payload);
    }

    @Override
    public UUID verify(String token) throws ServiceException {
        if (token == null || token.isBlank()) {
            throw invalidQr();
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 5 || !PREFIX.equals(parts[0]) || !VERSION.equals(parts[1])) {
            throw invalidQr();
        }
        String payload = parts[1] + "." + parts[2] + "." + parts[3];
        if (!MessageDigest.isEqual(
                sign(payload).getBytes(StandardCharsets.UTF_8),
                parts[4].getBytes(StandardCharsets.UTF_8)
        )) {
            throw invalidQr();
        }
        try {
            UUID bookingId = UUID.fromString(parts[2]);
            long expiresAt = Long.parseLong(parts[3]);
            if (clock.instant().getEpochSecond() > expiresAt) {
                throw new ServiceException("Mã QR đã hết hạn.");
            }
            return bookingId;
        } catch (IllegalArgumentException exception) {
            throw invalidQr();
        }
    }

    private String sign(String payload) throws ServiceException {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ServiceException("Không thể ký mã QR booking.");
        }
    }

    private ServiceException invalidQr() {
        return new ServiceException("Mã QR không hợp lệ hoặc đã bị thay đổi.");
    }
}
