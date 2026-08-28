package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateVenueCourtRequest {
    @NotNull(message = "ID sân không được để trống")
    private String venueId;
    @NotNull(message = "Loại thể thao không được để trống")
    private String sportType;
    @NotBlank(message = "Tên sân con không được để trống")
    private String name;
    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu là 1 người")
    private Integer capacity;
    private String surfaceType;
}
