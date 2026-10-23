package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Nhập sân OSM của một khu vực, vd TP. Hồ Chí Minh với khung nam - tây - bắc - đông của thành phố. */
public record ImportExternalVenuesRequest(
        @NotBlank(message = "Tên khu vực không được để trống")
        @Size(max = 100, message = "Tên khu vực tối đa 100 ký tự")
        String region,
        @NotNull Double south,
        @NotNull Double west,
        @NotNull Double north,
        @NotNull Double east
) {}
