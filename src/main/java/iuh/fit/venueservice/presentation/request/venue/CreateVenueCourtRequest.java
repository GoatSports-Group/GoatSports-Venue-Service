package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVenueCourtRequest {
    @NotNull(message = "ID sân không được để trống")
    private String venueId;
    @NotNull(message = "Loại thể thao không được để trống")
    private String sportType;
    @NotBlank(message = "Tên sân nhỏ (Court) không được để trống")
    private String name;
    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu là 1 người")
    private Integer capacity;
    @NotNull(message = "Giá mỗi giờ không được để trống")
    @Min(value = 0, message = "Giá mỗi giờ không được âm")
    private Double pricePerHour;
    @NotNull(message = "Thông tin trong nhà/ngoài trời không được để trống")
    private Boolean indoor;
}
