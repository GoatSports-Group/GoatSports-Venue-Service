package iuh.fit.venueservice.application.dto.response.venue;

import iuh.fit.venueservice.domain.entity.ExternalVenue;

/**
 * Khối "Nhận quản lý sân có sẵn" khi admin duyệt hồ sơ: sân ngoài được nhận, vị trí trong hồ sơ cách sân bao xa
 * ({@code distanceMeters} null khi hồ sơ không có toạ độ), nhu cầu đang chờ, và còn hồ sơ khác nhận cùng sân không.
 */
public record ExternalVenueClaimReview(
        ExternalVenue venue,
        Integer distanceMeters,
        boolean farAway,
        long wantCount,
        long matchCount,
        long otherPendingClaims
) {}
