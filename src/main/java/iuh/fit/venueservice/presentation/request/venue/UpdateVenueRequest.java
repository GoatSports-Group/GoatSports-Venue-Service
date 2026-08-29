package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVenueRequest {
    @NotBlank(message = "Tên cơ sở không được để trống")
    @Size(max = 255, message = "Tên cơ sở không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @NotNull(message = "Giờ mở cửa không được để trống")
    private LocalTime openTime;

    @NotNull(message = "Giờ đóng cửa không được để trống")
    private LocalTime closeTime;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;

    @NotNull(message = "Giá tối thiểu không được để trống")
    @DecimalMin(value = "0", message = "Giá tối thiểu phải lớn hơn hoặc bằng 0")
    private Double minPrice;

    @NotNull(message = "Giá tối đa không được để trống")
    @DecimalMin(value = "0", message = "Giá tối đa phải lớn hơn hoặc bằng 0")
    private Double maxPrice;

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự")
    private String phone;

    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @Size(max = 255, message = "Phường/xã không được vượt quá 255 ký tự")
    private String ward;

    @Size(max = 255, message = "Quận/huyện không được vượt quá 255 ký tự")
    private String district;

    @NotBlank(message = "Tỉnh/thành phố không được để trống")
    @Size(max = 255, message = "Tỉnh/thành phố không được vượt quá 255 ký tự")
    private String city;

    @DecimalMin(value = "-90", message = "Vĩ độ phải từ -90 đến 90")
    @DecimalMax(value = "90", message = "Vĩ độ phải từ -90 đến 90")
    private Double latitude;

    @DecimalMin(value = "-180", message = "Kinh độ phải từ -180 đến 180")
    @DecimalMax(value = "180", message = "Kinh độ phải từ -180 đến 180")
    private Double longitude;

    @Size(max = 12, message = "Chỉ được lưu tối đa 12 hình ảnh")
    private List<@NotBlank(message = "Đường dẫn hình ảnh không được để trống")
            @Size(max = 2048, message = "Đường dẫn hình ảnh quá dài") String> imageUrls;

    @Size(max = 30, message = "Chỉ được lưu tối đa 30 tiện ích")
    private List<@NotBlank(message = "Tên tiện ích không được để trống")
            @Size(max = 100, message = "Tên tiện ích không được vượt quá 100 ký tự") String> amenities;
}
