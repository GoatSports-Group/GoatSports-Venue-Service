package iuh.fit.venueservice.presentation.request.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentsRequest {
    @NotBlank(message = "Mã đơn đăng ký không được để trống")
    private String ownerApplicationId;
    @NotNull(message = "Danh sách tài liệu không được trống")
    private List<@NotNull(message = "Tài liệu không được trống") String> documentKeys;
}
