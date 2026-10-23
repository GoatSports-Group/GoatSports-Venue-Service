package iuh.fit.venueservice.application.dto.response.address;

import iuh.fit.venueservice.domain.entity.GeoBounds;

/** {@code bounds}: khung bao của địa danh (tỉnh, phường...) nếu Nominatim trả; admin dùng để chọn vùng nhập sân ngoài. */
public record GeocodingResultCommand(
        double latitude,
        double longitude,
        String title,
        String address,
        GeoBounds bounds
) {
    public GeocodingResultCommand(double latitude, double longitude, String title, String address) {
        this(latitude, longitude, title, address, null);
    }
}
