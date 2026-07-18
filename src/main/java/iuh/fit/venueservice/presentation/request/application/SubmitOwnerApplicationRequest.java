package iuh.fit.venueservice.presentation.request.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitOwnerApplicationRequest {
    @NotBlank(message = "Mã người dùng không được để trống")
    private String userId;
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
    @NotBlank(message = "Số CCCD/Hộ chiếu không được để trống")
    private String identityNumber;
    @NotBlank(message = "Tên doanh nghiệp/Hộ kinh doanh không được để trống")
    private String businessName;
    @NotNull(message = "Loại hình kinh doanh không được để trống")
    private String businessType;
    @NotBlank(message = "Mã số thuế không được để trống")
    private String taxCode;
    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String address;
    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;
    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;
    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;
    @NotBlank(message = "Thành phố không được để trống")
    private String city;
}
