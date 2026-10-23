package iuh.fit.venueservice.domain.entity;

import java.util.ArrayList;
import java.util.List;

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

    /** Chia khung thành các ô tối đa {@code size} độ mỗi chiều; ô ở mép bắc / đông bị cắt theo khung. */
    public List<GeoBounds> tiles(double size) {
        List<GeoBounds> tiles = new ArrayList<>();
        int rows = (int) Math.ceil(heightDegrees() / size);
        int columns = (int) Math.ceil(widthDegrees() / size);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                double tileSouth = south + row * size;
                double tileWest = west + column * size;
                tiles.add(new GeoBounds(tileSouth, tileWest,
                        Math.min(north, tileSouth + size), Math.min(east, tileWest + size)));
            }
        }
        return tiles;
    }
}
