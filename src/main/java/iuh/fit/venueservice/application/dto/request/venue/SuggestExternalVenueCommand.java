package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.domain.enums.SportType;

import java.util.Set;

/** Người chơi gợi ý một sân còn thiếu trên bản đồ; địa chỉ, số điện thoại, ghi chú có thể trống. */
public record SuggestExternalVenueCommand(
        String name,
        Set<SportType> sportTypes,
        double latitude,
        double longitude,
        String address,
        String phone,
        String note
) {}
