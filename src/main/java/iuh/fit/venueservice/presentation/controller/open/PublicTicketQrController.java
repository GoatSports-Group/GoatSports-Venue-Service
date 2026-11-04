package iuh.fit.venueservice.presentation.controller.open;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.BookingQrPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/venues/ticket-qr")
@RequiredArgsConstructor
public class PublicTicketQrController {
    private static final int QR_SIZE = 360;

    private final BookingQrPort bookingQrPort;

    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@RequestParam String code) throws ServiceException {
        bookingQrPort.verify(code);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .contentType(MediaType.IMAGE_PNG)
                .body(png(code));
    }

    /** QR đen trên nền trắng, giữ vùng trắng 4 ô mặc định của zxing để máy quét của chủ sân đọc được. */
    static byte[] png(String content) throws ServiceException {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < QR_SIZE; x++) {
                for (int y = 0; y < QR_SIZE; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new ServiceException("Không vẽ được mã QR vé đặt sân.");
        }
    }
}
