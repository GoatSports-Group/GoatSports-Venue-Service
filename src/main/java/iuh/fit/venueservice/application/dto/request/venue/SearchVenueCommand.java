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
        /** Ket qua semantic search, da sap theo do lien quan. null = khong dung semantic. */
        List<UUID> semanticVenueIds
) {
    public SearchVenueCommand(
            String keyword, SportType sportType, String district, String city,
            Double latitude, Double longitude, Double radiusKm,
            Double northLatitude, Double southLatitude, Double eastLongitude, Double westLongitude,
            Double minPrice, Double maxPrice, Double minRating, int page, int size
    ) {
        this(keyword, sportType, district, city, latitude, longitude, radiusKm,
                northLatitude, southLatitude, eastLongitude, westLongitude,
                minPrice, maxPrice, minRating, page, size, null);
    }

    public SearchVenueCommand withSemanticVenueIds(List<UUID> venueIds) {
        return new SearchVenueCommand(keyword, sportType, district, city, latitude, longitude, radiusKm,
                northLatitude, southLatitude, eastLongitude, westLongitude,
                minPrice, maxPrice, minRating, page, size, venueIds);
    }

    public boolean usesSemanticSearch() {
        return semanticVenueIds != null;
    }
}
