package iuh.fit.venueservice.presentation.request.venue;

import iuh.fit.venueservice.domain.enums.SportType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Người chơi gợi ý một sân thực tế chưa có trên bản đồ; vị trí là điểm họ chạm trên bản đồ. */
public record SuggestExternalVenueRequest(
        @NotBlank(message = "Nhập tên sân")
        @Size(min = 3, max = 200, message = "Tên sân từ 3 đến 200 ký tự")
        String name,
        @NotEmpty(message = "Chọn ít nhất một môn")
        Set<SportType> sportTypes,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự") String address,
        @Size(max = 60, message = "Số điện thoại tối đa 60 ký tự") String phone,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note
) {}
