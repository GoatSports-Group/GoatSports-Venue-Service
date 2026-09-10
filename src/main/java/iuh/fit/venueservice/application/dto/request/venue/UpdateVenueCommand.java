package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record UpdateVenueCommand(
        UUID venueId,
        String name,
        String description,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean active,
        Double minPrice,
        Double maxPrice,
        String phone,
        String email,
        String address,
        String ward,
        String district,
        String city,
        Double latitude,
        Double longitude,
        List<String> imageUrls,
        List<String> amenities,
        UUID currentUserId
) {
    public void validate() throws ServiceException {
        if (!openTime.isBefore(closeTime)) {
            throw new ServiceException("Giờ đóng cửa phải sau giờ mở cửa.");
        }
        if (minPrice > maxPrice) {
            throw new ServiceException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }
    }
}
