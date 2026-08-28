package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class CreateBookingRequest {
    @NotNull(message = "ID sân con không được để trống.")
    private UUID venueCourtId;
    @NotNull(message = "Ngày chơi không được để trống.")
    private LocalDate playDate;
    @NotNull(message = "Giờ bắt đầu không được để trống.")
    private LocalTime startTime;
    @NotNull(message = "Giờ kết thúc không được để trống.")
    private LocalTime endTime;
}
