package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.SportType;

import java.util.List;
import java.util.UUID;

public record SearchVenueCommand(
        String keyword,
        SportType sportType,
        String district,
        String city,
        Double latitude,
        Double longitude,
        Double radiusKm,
        Double northLatitude,
        Double southLatitude,
        Double eastLongitude,
        Double westLongitude,
        Double minPrice,
        Double maxPrice,
        Double minRating,
        int page,
        int size,
        /** Ket qua semantic search: GOP voi keyword LIKE (OR), khong thay the. null = khong dung semantic. */
        List<UUID> semanticVenueIds,
        /** Chi lay cac san nay (vd. trang "Sân đã lưu"): gioi han cung, AND voi moi bo loc. null = khong gioi han. */
        List<UUID> venueIds,
        /** Thứ tự kết quả; null = mặc định (gần nhất khi tìm theo bán kính). */
        Sort sort
) {
    /** PRICE: giá thấp nhất trước; RATING: đánh giá cao trước, cùng điểm thì nhiều đánh giá hơn trước. */
    public enum Sort { PRICE, RATING }

    public SearchVenueCommand(
            String keyword, SportType sportType, String district, String city,
            Double latitude, Double longitude, Double radiusKm,
            Double northLatitude, Double southLatitude, Double eastLongitude, Double westLongitude,
            Double minPrice, Double maxPrice, Double minRating, int page, int size
    ) {
        this(keyword, sportType, district, city, latitude, longitude, radiusKm,
                northLatitude, southLatitude, eastLongitude, westLongitude,
                minPrice, maxPrice, minRating, page, size, null, null, null);
    }

    public SearchVenueCommand withSemanticVenueIds(List<UUID> venueIds) {
        return new SearchVenueCommand(keyword, sportType, district, city, latitude, longitude, radiusKm,
                northLatitude, southLatitude, eastLongitude, westLongitude,
                minPrice, maxPrice, minRating, page, size, venueIds, this.venueIds, sort);
    }

    public SearchVenueCommand withVenueIds(List<UUID> ids) {
        return new SearchVenueCommand(keyword, sportType, district, city, latitude, longitude, radiusKm,
                northLatitude, southLatitude, eastLongitude, westLongitude,
                minPrice, maxPrice, minRating, page, size, semanticVenueIds, ids, sort);
    }

    public SearchVenueCommand withSort(Sort order) {
        return new SearchVenueCommand(keyword, sportType, district, city, latitude, longitude, radiusKm,
                northLatitude, southLatitude, eastLongitude, westLongitude,
                minPrice, maxPrice, minRating, page, size, semanticVenueIds, venueIds, order);
    }

    public boolean usesSemanticSearch() {
        return semanticVenueIds != null;
    }
}
