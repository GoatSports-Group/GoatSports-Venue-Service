package iuh.fit.venueservice.application.port.persistence;

import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueRegionSummary;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.enums.SportType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalVenueRepository {
    /** Thêm mới hoặc cập nhật theo (source, sourceId); giữ nguyên id của sân đã có. Trả số bản ghi đã lưu. */
    int upsertAll(List<ExternalVenue> venues);

    /** {@code sportType}, {@code keyword} có thể null; tối đa {@code limit} sân, theo tên. */
    List<ExternalVenue> findInBounds(GeoBounds bounds, SportType sportType, String keyword, int limit);

    Optional<ExternalVenue> findById(UUID externalVenueId);

    List<ExternalVenueRegionSummary> summarizeByRegion();
}
