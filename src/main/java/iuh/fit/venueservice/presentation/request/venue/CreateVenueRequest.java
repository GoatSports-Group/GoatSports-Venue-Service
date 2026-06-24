package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVenueRequest {
    @NotBlank(message = "Tên sân không được để trống")
    private String name;
    private String description;
    @NotNull(message = "Giờ mở cửa không được để trống")
    private LocalDateTime openTime;
    @NotNull(message = "Giờ đóng cửa không được để trống")
    private LocalDateTime closeTime;
    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
    @NotNull(message = "Giá tối đa không được để trống")
    @Min(value = 0, message = "Giá tối thiểu phải lớn hơn hoặc bằng 0")
    private Double minPrice;
    @NotNull(message = "Giá tối đa không được để trống")
    @Min(value = 0, message = "Giá tối đa phải lớn hơn hoặc bằng 0")
    private Double maxPrice;
    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    private String phone;
    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
}
