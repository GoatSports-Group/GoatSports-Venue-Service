package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePlayerReviewRequest(
        @NotNull(message = "Booking là bắt buộc") UUID bookingId,
        @NotNull(message = "Số sao là bắt buộc")
        @Min(value = 1, message = "Số sao tối thiểu là 1")
        @Max(value = 5, message = "Số sao tối đa là 5") Integer rating,
        @Size(max = 2000, message = "Nội dung đánh giá tối đa 2000 ký tự") String content
) {
}
