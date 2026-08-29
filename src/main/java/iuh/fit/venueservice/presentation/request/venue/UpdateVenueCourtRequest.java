package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVenueCourtRequest {
    @NotBlank(message = "Tên sân thi đấu không được để trống")
    @Size(max = 255, message = "Tên sân thi đấu không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Môn thể thao không được để trống")
    private String sportType;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu là 1 người")
    private Integer capacity;

    @Size(max = 255, message = "Loại mặt sân không được vượt quá 255 ký tự")
    private String surfaceType;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}
