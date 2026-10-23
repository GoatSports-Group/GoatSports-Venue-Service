package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.constraints.Size;

/** Lý do (không bắt buộc) gửi kèm thông báo cho người gợi ý. */
public record RejectExternalVenueSuggestionRequest(
        @Size(max = 500, message = "Lý do tối đa 500 ký tự") String reason
) {}
