package iuh.fit.venueservice.domain.entity;

/** Khung bản đồ: nam - tây - bắc - đông (độ). */
public record GeoBounds(double south, double west, double north, double east) {

    public boolean isValid() {
        return south >= -90 && north <= 90 && west >= -180 && east <= 180 && south < north && west < east;
    }

    public double heightDegrees() {
        return north - south;
    }

    public double widthDegrees() {
        return east - west;
    }
}
