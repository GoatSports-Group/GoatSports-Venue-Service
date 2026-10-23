package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.GeoBounds;

import java.util.List;

/** Nguồn sân ngoài thực tế (OpenStreetMap). Chỉ trả sân có tên, có môn GOAT Sports hỗ trợ, không ghi là riêng tư. */
public interface ExternalVenueSourcePort {
    List<ExternalVenue> fetch(GeoBounds bounds);
}
